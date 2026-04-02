import {do_request} from "./fetch_request.js";

const content = document.querySelector(".content");
document.querySelectorAll('input[name = "chapter"]').forEach(radio =>{
    radio.addEventListener("change",()=>load_content(radio.value))
})

document.addEventListener('DOMContentLoaded',async ()=>{
    const checkedRadio = document.querySelector('input[name="chapter"]:checked');
    if (checkedRadio) {
        await load_content(checkedRadio.value);
    }
})

async function load_content(value) {
    if(value == "do_order"){
        await get_order_menu();
    }
    else{
        await get_my_order();
    }
}

async function get_order_menu(){
    const template = document.getElementById("order-content")
    const content_order = template.content.cloneNode(true);
    const service_list = content_order.querySelector(".service-list");
    const responce = await serviceFetch();
    const services = await responce.json();
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
        service.addEventListener('click',()=>{
            service.classList.toggle("selected");
        })
    })
    const old_make_order_but = content_order.querySelector(".order-button");
    const new_make_order_but = old_make_order_but.cloneNode(true);
    old_make_order_but.replaceWith(new_make_order_but);
    new_make_order_but.addEventListener('click',make_order)
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

    const responce = await do_request(url,body);
    return responce;
}

async function make_order() {
    const brandInput = document.getElementById("car-brand");
    const modelInput = document.getElementById("car-model");
    const numberInput = document.getElementById("car-number");
    const serviceCards = document.querySelectorAll(".service-card.selected");

    const make = brandInput.value.trim();
    const model = modelInput.value.trim();
    const number = numberInput.value.trim();

    brandInput.style.border = "";
    modelInput.style.border = "";
    numberInput.style.border = "";

    if (make === "") {
        brandInput.style.border = "2px solid red";
        alert("Заполните поле.");
        return;
    }

    if (model === "") {
        modelInput.style.border = "2px solid red";
        alert("Заполните поле.");
        return;
    }
    if (number === "") {
        numberInput.style.border = "2px solid red";
        alert("Заполните поле.");
        return;
    }

    if (serviceCards.length === 0) {
        alert("Выберите хотя бы одну услугу.");
        return;
    }

    const service_id = Array.from(serviceCards).map(s=>s.dataset.id);
    const responce = await do_order_reguest(make,model,number,service_id);
    if(responce.ok){
        alert("Ваш заказ успешно оформлен");
        brandInput.value = "";
        modelInput.value = "";
        numberInput.value = "";
        serviceCards.forEach(element => {
            element.classList.remove("selected")
        })
    }
    else{
        alert("Что то пошло не так попробуйте позже");
    }
}

async function do_order_reguest(make,model,number,list_id) {
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
            'serviceId' : list_id
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
