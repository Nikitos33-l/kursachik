import {do_request} from "./fetch_request.js";

 const content = document.querySelector(".content");
 const navContainer = document.querySelector(".chose_chapter-container"); // Та самая синяя полоса с кнопками

 // 1. ИЗМЕНЯЕМ ТОЧКУ ВХОДА
 document.addEventListener('DOMContentLoaded', () => {
     showMainMenu(); // Вместо загрузки вкладок, всегда показываем приветствие
 })

 function showMainMenu() {
     if(navContainer) navContainer.style.display = "none";

     content.innerHTML = "";
     const template = document.getElementById("main-menu-template");

     content.appendChild(template.content.cloneNode(true));

     document.getElementById("btn-choose-sto").onclick = get_stations_menu;
     document.getElementById("btn-view-orders").onclick = () => {
         navContainer.style.display = "flex";
         document.getElementById("user_but").checked = true;
         get_my_order();
     };
 }

async function get_stations_menu() {
    content.innerHTML = '<h2 class="loading-title" style="text-align:center; padding: 50px;">Загрузка филиалов...</h2>';
    const mockStations = [
            { id: 1, name: 'СТО "Центральный"', address: 'ул. Ленина, 45', lat: 53.8985, lng: 27.5585 },
            { id: 2, name: 'СТО "Северный"', address: 'пр. Независимости, 120', lat: 53.9350, lng: 27.6500 },
            { id: 3, name: 'СТО "Южный"', address: 'ул. Победы, 88', lat: 53.8500, lng: 27.5200 },
            { id: 4, name: 'СТО "Восточный"', address: 'ул. Партизанская, 14', lat: 53.8800, lng: 27.6200 },
            { id: 5, name: 'СТО "Западный"', address: 'пр. Машерова, 33', lat: 53.9100, lng: 27.5400 }
        ];

    //const response = await do_request("http://localhost:8080/api/stations/getAll", { method: "GET" });
    const stations = mockStations

    content.innerHTML = "";
    const template = document.getElementById("sto-selection-template");
    content.appendChild(template.content.cloneNode(true));

    const listContainer = document.getElementById("sto-list-container");
    const continueBtn = document.getElementById("continue-sto-btn");
    const continueBtnText = document.getElementById("continue-btn-text");

    let activeStationId = null;
    let map;
    let placemarks = {};

    ymaps.ready(() => {
        map = new ymaps.Map("yandex-map", {
            center: [53.9045, 27.5615],
            zoom: 11,
            controls: ['zoomControl']
        });

        stations.forEach(sto => {
            const stoCard = document.createElement("div");
            stoCard.className = "sto-card";
            stoCard.dataset.id = sto.id;
            stoCard.innerHTML = `
                <div class="sto-card-icon">➤</div>
                <div class="sto-card-info">
                    <h4>${sto.name}</h4>
                    <p>${sto.address}</p>
                </div>
            `;
            listContainer.appendChild(stoCard);

            const coords = [
                sto.lat,
                sto.lng
            ];

            const placemark = new ymaps.Placemark(coords, {
                hintContent: sto.name,
                balloonContent: `<b>${sto.name}</b><br>${sto.address}`
            }, {
                preset: 'islands#grayIcon'
            });

            placemarks[sto.id] = placemark;
            map.geoObjects.add(placemark);

            const selectSto = () => {
                activeStationId = sto.id;

                document.querySelectorAll('.sto-card').forEach(c => c.classList.remove('active'));

                stoCard.classList.add('active');

                Object.values(placemarks).forEach(p => {
                    p.options.set('preset', 'islands#grayIcon');
                });

                placemark.options.set('preset', 'islands#blueIcon');
                map.panTo(coords, {duration: 300});
                placemark.balloon.open();

                continueBtnText.textContent = `Продолжить с ${sto.name}`;
                continueBtn.style.display = "flex";
            };

            stoCard.addEventListener("click", selectSto);

            placemark.events.add('click', selectSto);
        });
    });

    continueBtn.addEventListener("click", () => {
        if (activeStationId) {
            localStorage.setItem("selectedStationId", activeStationId);
            navContainer.style.display = "flex";
            document.getElementById("order_but").checked = true;
            load_content("do_order")
        }
    });
}

async function load_content(value) {
    if(value == "do_order"){
        if(!localStorage.getItem("selectedStationId")) {
            get_stations_menu();
        } else {
            await get_order_menu();
        }
    }
    else{
        await get_my_order();
    }
}


async function do_order_reguest(make, model, number, list_id) {
    const stationId = localStorage.getItem("selectedStationId");
    const url = "http://localhost:8080/api/order/create";

    const body = {
        method : 'POST',
        headers : {
            'Autorization' : `Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type' : 'application/json'
        },
        body : JSON.stringify({
            'vehicle' : {
                'make' : make,
                'model' : model,
                'number' : number
            },
            'serviceId' : list_id,
            'stationId' : stationId
        })
    }

   const responce = await do_request(url,body);
   return responce;
}

async function get_my_order() {
    content.innerHTML = "";
    const responce = await do_my_order_reguest();
    const orders = await responce.json();
    if (!orders || orders.length === 0) {
        const template = document.getElementById("no_order_content");
        const no_order_content = template.content.cloneNode(true);
        content.appendChild(no_order_content);
        return;
    }
    orders.forEach(o=>{
        const card = renderOrderCard(o);
        content.appendChild(card);
    })
}

async function do_my_order_reguest() {
    const url = "http://localhost:8080/api/order/getClientOrder";
    const body = {
        method : "GET",
        headers : {
            'Content-Type' : 'application/json',
            'Autorization' : `Bearer ${localStorage.getItem("acesstoken")}`
        }
    };
    const responce = await do_request(url,body);
    return responce;
}

function renderOrderCard(order) {
    const template = document.getElementById("order-card-content");
    const my_order_content = template.content.cloneNode(true);
    const vehicle_name = my_order_content.querySelector(".car-model");
    vehicle_name.textContent = `${order.vehicle.make}   ${order.vehicle.model}`
    const vehicle_number = my_order_content.querySelector(".car-plate");
    vehicle_number.textContent = order.vehicle.number;
    const status = my_order_content.querySelector(".car-status");
    status.textContent = order.status;
    status.classList.add(getStatusClass(order.status));
    const service_list = my_order_content.querySelector(".order-service-list");
    let res_price = 0;
    order.services.forEach(s=>{
        res_price += s.price;
        const card = document.createElement('div');
        card.classList.add("order-service-card");
        const span = document.createElement('span');
        span.classList.add("order-service-name");
        span.textContent = s.name;
        card.appendChild(span);
        const price_span =  document.createElement('span');
        price_span.classList.add("order-service-price");
        price_span.textContent = `${s.price} BYN`
        card.appendChild(price_span);   
        service_list.appendChild(card);
    })
    my_order_content.querySelector(".order-total-value").textContent = `${res_price} BYN`;
    return my_order_content;
  }
  
function getStatusClass(status) {
    switch (status) {
      case "Новый": return "status-new";
      case "В работе" : return "status-inprogress"
      case "Выполнен": return "status-completed";
      case "Отменён": return "status-cancelled";
      default: return "";
    }
}
async function get_order_menu(){
    const template = document.getElementById("order-content");
    const content_order = template.content.cloneNode(true);
    const service_list = content_order.querySelector(".service-list");

    const response = await serviceFetch();
    const services = await response.json();

    service_list.innerHTML = "";
    services.forEach(element =>{
        const service = document.createElement("div");
        service.classList.add("service-card");
        service.dataset.id = element.id;

        service.innerHTML = `
            <span class="checkmark"></span>
            <span class="service-name">${element.name}</span>
            <span class="service-price">${element.price} BYN</span>
        `;

        service_list.appendChild(service);
        service.addEventListener('click', () => {
            service.classList.toggle("selected");
        });
    });

    const old_make_order_but = content_order.querySelector(".order-button");
    const new_make_order_but = old_make_order_but.cloneNode(true);
    old_make_order_but.replaceWith(new_make_order_but);
    new_make_order_but.addEventListener('click', make_order);

    content.innerHTML = "";
    content.appendChild(content_order);
}

async function serviceFetch() {
    const url = "http://localhost:8080/api/service/getAll";
    const body = {
        method : "GET",
        headers : {
            'Autorization' : `Bearer ${localStorage.getItem("acesstoken")}`
        }
    };

    return await do_request(url, body);
}

async function make_order() {
    const brandInput = document.getElementById("car-brand");
    const modelInput = document.getElementById("car-model");
    const numberInput = document.getElementById("car-number");
    const serviceCards = document.querySelectorAll(".service-card.selected");

    const make = brandInput.value.trim();
    const model = modelInput.value.trim();
    const number = numberInput.value.trim();

    [brandInput, modelInput, numberInput].forEach(el => el.style.border = "");

    if (!make || !model || !number) {
        if (!make) brandInput.style.border = "2px solid red";
        if (!model) modelInput.style.border = "2px solid red";
        if (!number) numberInput.style.border = "2px solid red";
        alert("Заполните все поля автомобиля.");
        return;
    }

    if (serviceCards.length === 0) {
        alert("Выберите хотя бы одну услугу.");
        return;
    }

    const service_ids = Array.from(serviceCards).map(s => s.dataset.id);

    const response = await do_order_reguest(make, model, number, service_ids);

    if(response.ok){
        alert("Ваш заказ успешно оформлен!");
        // Очистка полей
        brandInput.value = "";
        modelInput.value = "";
        numberInput.value = "";
        serviceCards.forEach(el => el.classList.remove("selected"));

        // После успеха можно отправить пользователя в "Мои заказы"
        document.getElementById("user_but").checked = true;
        load_content("my_orders");
    } else {
        alert("Что-то пошло не так, попробуйте позже.");
    }
}
