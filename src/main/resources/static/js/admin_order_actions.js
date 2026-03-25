import {do_request} from "./fetch_request.js"

let document;

export async function load_orderContent(document_page){
    document = document_page;
    const content = document.querySelector('.content');
    const order_template = document.querySelector(".order-content");
    const item = order_template.content.cloneNode(true);
    const response = await get_orders();
    if(response.ok){
        const orders = await response.json();
        const tbody = item.querySelector("tbody");
        orders.forEach(order => {
            const row = document.createElement("tr");

            let sum = 0;
            order.services.forEach(element=>{
                sum += element.price;
            })
            let servicesText = "";
            if (order.services && order.services.length > 0) {
              servicesText = order.services[0].name;
              if (order.services.length > 1) {
                servicesText += ` +${order.services.length - 1}`;
              }
            }
          
            const workersCount = order.workers ? order.workers.length : 0;
            const workersHTML = `
              <span>${workersCount}</span>
              <img src="icons/worker.png" alt="worker" style="width:17px;height:17px;margin-left:4px;">
            `;

            row.innerHTML = `
            <td>${order.client.name}<br>${order.client.email}</td>
            <td>${order.venicle.make}  ${order.venicle.model}</td>
            <td>${servicesText}</td>
            <td><span class="status-tag ${status_in_normal(order.status)}">${order.status}</span></td>
            <td>${workersHTML}</td>
            <td>${sum} BYN</td>
            <td>
            <button class="action-view" data-id="${order.id}"><img src="icons/view.png" alt="view" style="width:17px;height:17px"></button>
            <button class="action-edit" data-id="${order.id}"><img src="icons/change.png" alt="view" style="width:17px;height:17px"></button>
            </td>
            `
            tbody.appendChild(row);
        })
        content.innerHTML = "";
        content.appendChild(item);
        document.querySelectorAll(".action-view").forEach(element=>{
            element.addEventListener('click',()=>view_details(element.dataset.id))
        })
        document.querySelectorAll(".action-edit").forEach(element=>{
            element.addEventListener('click',()=>redact_order(element.dataset.id,document.getElementById("editModal")))
        })
    }
}
       
async function view_details(id){
    const response = await get_details(id);
    const order = await response.json();
    const template = document.querySelector(".view-details");
    const details = template.content.cloneNode(true);
    details.querySelector(".client-name").textContent = order.client.name;
    details.querySelector(".client-email").textContent = order.client.email;
    details.querySelector(".venicle").textContent = `${order.venicle.make},${order.venicle.model}  Госномер:${order.venicle.number}`;
    let sum = 0;
    const service_list = details.querySelector(".service-list");
    service_list.innerHTML = "";
    if(order.services.length > 0){
        order.services.forEach(service => {
            const card = document.createElement("div");
            card.className = "service-card";
    
            const name = document.createElement("div");
            name.className = "service-name";
            name.textContent = service.name;
    
            const price = document.createElement("div");
            price.className = "service-price";
            price.textContent = `${service.price} BYN`;
    
            card.appendChild(name);
            card.appendChild(price);
            service_list.appendChild(card);
    
            sum += service.price;
        });
    }
    else {
        const p = document.createElement('p');
        p.textContent = "Услуги были удалены";
        service_list.appendChild(p);
    }
    
    const worker_list = details.querySelector(".workers-list");
    worker_list.innerHTML = "";
    
    if (order.workers.length > 0) {
      order.workers.forEach(worker => {
        const card = document.createElement("div");
        card.className = "worker-card";
    
        const name = document.createElement("div");
        name.className = "worker-name";
        name.textContent = worker.name;
    
        const email = document.createElement("div");
        email.className = "worker-email";
        email.textContent = worker.email;
    
        card.appendChild(name);
        card.appendChild(email);
        worker_list.appendChild(card);
      });
    } else {
      const info = document.createElement("p");
      info.textContent = "Рабочие пока не назначены";
      info.style.color = "#777";
      info.style.fontStyle = "italic";
      worker_list.appendChild(info);
    }
    
    details.querySelector(".status").innerHTML = `<span class='status-tag ${status_in_normal(order.status)}'>${order.status}</span>`
    details.querySelector(".price").textContent = `${sum} BYN`;
    const details_container = document.querySelector(".order-details");
    details_container.innerHTML = "";
    details_container.appendChild(details);
    const change_button = document.querySelector(".change-button");
    const new_change_button = change_button.cloneNode(true);
    change_button.replaceWith(new_change_button);
    new_change_button.addEventListener('click',()=>redact_order(id,document.getElementById("editModal")))
}

function status_in_normal(status){
    if(status == "В работе"){
        return "В_работе"
    }
    else{
        return status;
    }
}

async function redact_order(id,modal) {
    const order_details = await get_details(id);
    modal.style.display = 'flex';
    const order = await order_details.json();
    modal.querySelector(".name").textContent = order.client.name;
    modal.querySelector(".email").textContent = order.client.email;
    modal.querySelector(".venicle-modal-info").textContent = `${order.venicle.make}, ${order.venicle.model} Госномер:${order.venicle.number}`;
    const service_list = modal.querySelector(".service-list");
    service_list.innerHTML = "";
    if(order.services.length > 0){
        order.services.forEach(element=>{
            const card = document.createElement("div");
            card.className = "service-card";
    
            const name = document.createElement("div");
            name.className = "service-name";
            name.textContent = element.name;
    
            const price = document.createElement("div");
            price.className = "service-price";
            price.textContent = `${element.price} BYN`;
            card.appendChild(name);
            card.appendChild(price);
            service_list.appendChild(card);
        })
    }
    else{
        const p = document.createElement('p');
        p.textContent = "Услуги были удалены";
        service_list.appendChild(p);
    }

    const worker_list = modal.querySelector(".workers-box");
    const worker_response = await get_workers();
    const workers = await worker_response.json();
    if(workers != null){
        render_workers(workers,worker_list,order.workers);
    }
    else{
        worker_list.innerHTML = "";
        const p = document.createElement('p');
        p.textContent = "Рабочих пока нету";
        worker_list.appendChild(p);
    }
    const status_list = modal.querySelector(".dropdown-status");
    const status_response = await get_status();
    const all_status = await status_response.json();
    status_list.innerHTML = "";
    all_status.forEach(element=>{
        const option = document.createElement('option');
        option.value = element.id;
        option.textContent = element.name;
        if(element.name == order.status){
            option.selected = true;
        }
        status_list.appendChild(option);
    })
    const redact_button = modal.querySelector(".save-button");
    redact_button.addEventListener('click',()=>save_changes(id,modal), { once: true })
    const cansel_button = modal.querySelector(".cansel-button");
    cansel_button.addEventListener('click',()=>modal.style.display = 'none',{ once: true })
}

async function get_orders(){
    const url = "http://localhost:8080/api/order/getAll";
    const body = {
        method : "GET",
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    };
    const response = await do_request(url,body);
    return response;
}

async function save_changes(order_id,modal) {
    const checkboxes = modal.querySelectorAll(".workers-box input[type = 'checkbox']");
    const workers_id = Array.from(checkboxes).filter(c=>c.checked).map(c=>c.value);
    const select = modal.querySelector(".dropdown-status");
    const status = select.value;
    const body = {
        method: 'PUT',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body:JSON.stringify({
            'workers_id': workers_id,
            'status_id': status
        })
    }
    console.log(body);
    const url = `http://localhost:8080/api/order/update_order/${order_id}`;
    const response = await do_request(url,body);
    if(response.ok){
        await load_orderContent(document);
        modal.style.display = 'none'
    }
}

async function get_details(id) {
    const url = `http://localhost:8080/api/order/get/${id}`;
    const body = {
        method:"GET",
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    const response = await do_request(url,body);
    return response;
}

async function get_status() {
    const body = {
        method:"GET",
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    const url = "http://localhost:8080/api/status/order/getAll";
    const response = await do_request(url,body);
    return response;
}

async function get_workers() {
    const url = "http://localhost:8080/api/user/get/all/workers";
    const body = {
        method:"GET",
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    };
    const response = await do_request(url,body);
    return response;
}

function render_workers(workers,worker_list,client_workers){
    worker_list.innerHTML = "";
    workers.forEach(worker=>{
        const label = document.createElement("label");
    label.classList.add("worker-item");
    
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    if(client_workers.length!=0 && client_workers.some(element=>element.id == worker.id)){
        checkbox.checked = true;
    }
    checkbox.value = worker.id;

    const info = document.createElement("div");
    info.classList.add("worker-info");

    const name = document.createElement("div");
    name.classList.add("name");
    name.textContent = worker.name;

    const email = document.createElement("div");
    email.classList.add("email");
    email.textContent = worker.email;

    info.appendChild(name);
    info.appendChild(email);

    label.appendChild(checkbox);
    label.appendChild(info);

    worker_list.appendChild(label);
    })
}
