import {do_request} from "./fetch_request.js"

let document

export async function load_serviceContent(document_page){
    document = document_page;
    const content = document.querySelector('.content');
    const template = document.querySelector(".service-content");
    const item = template.content.cloneNode(true);
    const add_service_button = item.querySelector(".add_button");
    add_service_button.addEventListener('click',add_service)
    const tbody = item.querySelector("tbody") 
    const responce = await get_services();
    const services = await responce.json();
    tbody.innerHTML = "";
    services.forEach(element =>{
        const row = document.createElement("tr");

            row.innerHTML = `
            <td>${element.name}</td>
            <td>${element.price} BYN</td>
            <td>
            <button class="action-edit" data-id="${element.id}"><img src="icons/change.png" alt="view" style="width:17px;height:17px"></button>
             <button class="action-view" data-id="${element.id}"><img src="icons/trash.png" alt="view" style="width:17px;height:17px"></button>
            </td>
            `
        tbody.appendChild(row);
        const del_button = row.querySelector(".action-view");
        del_button.addEventListener('click',()=>{
            delete_service(del_button.dataset.id);
        },{once:true})

        const redact_service_but = row.querySelector(".action-edit");
        redact_service_but.addEventListener('click',()=>{
            redact_service(redact_service_but.dataset.id);
        })
        
    })
    content.innerHTML = "";
    content.appendChild(item);
}

async function add_service() {
    const modal = document.getElementById("service-modal-add");
    modal.style.display = "flex";
    const nameInput = document.getElementById('service-modal-add-name');
    const priceInput = document.getElementById('service-modal-add-price');
    nameInput.value = ''
    priceInput.value = ''
    const oldAddBtn = document.getElementById("service-modal-add-btn");
    const newAddBtn = oldAddBtn.cloneNode(true);
    oldAddBtn.replaceWith(newAddBtn); 
    const handler = () => add(modal);
    newAddBtn.addEventListener("click", handler);
    const cancelBtn = document.getElementById("service-modal-add-cancel-btn");
    cancelBtn.addEventListener("click", () => {
        modal.style.display = "none";
        newAddBtn.removeEventListener("click", handler);
    });
}

async function add(modal) {
    const nameInput = document.getElementById('service-modal-add-name');
    const priceInput = document.getElementById('service-modal-add-price');

    nameInput.classList.remove("error");
    priceInput.classList.remove("error");
    
    const name = nameInput.value.trim();
    const price = parseFloat(priceInput.value);
    if (name === '') {
        nameInput.classList.add("error");
        alert('Пожалуйста, заполните  поле, оно не должны быть пустым.');
        return
    }

    if (isNaN(price) || price <= 0) {
        priceInput.classList.add("error");
        alert('Пожалуйста, заполните  поле, оно не должны быть пустым или отрицательным');
        return
    }
    await do_add_request(name,price);
    await load_serviceContent(document);
    modal.style.display = "none";
}

async function do_add_request(name,price) {
    const url = "http://localhost:8080/api/service/add";
    const body = {
        method:'POST',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body:JSON.stringify({
            'name':name,
            'price':price
        })
    }
    await do_request(url,body);
}

async function delete_service(id) {
    const url = `http://localhost:8080/api/service/del/${id}`;
    const body = {
        method : 'DELETE',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    await do_request(url,body);
    await load_serviceContent(document);
}

async function redact_service(id) {
    const responce = await get_service_info(id);
    const service = await responce.json();

    const modal = document.getElementById("service-modal");
    modal.style.display = "flex";

    const oldSaveBtn = document.getElementById("service-modal-save-btn");
    const newSaveBtn = oldSaveBtn.cloneNode(true);
    oldSaveBtn.replaceWith(newSaveBtn);

    document.getElementById("service-modal-price").value = service.price;
    document.getElementById("service-modal-name").value = service.name;

    const handler = () => save(id, modal);
    newSaveBtn.addEventListener("click", handler);

    const cancelBtn = document.getElementById("service-modal-cancel-btn");
    cancelBtn.addEventListener("click", () => {
        modal.style.display = "none";
        newSaveBtn.removeEventListener("click", handler);
    });
}

async function save(id,modal) {
    const nameInput = document.getElementById('service-modal-name');
    const priceInput = document.getElementById('service-modal-price');

    nameInput.classList.remove("error");
    priceInput.classList.remove("error");
    
    const name = nameInput.value.trim();
    const price = parseFloat(priceInput.value);

    if (name === '') {
        nameInput.classList.add("error");
        alert('Пожалуйста, заполните  поле, оно не должны быть пустым.');
        return
    }

    if (isNaN(price) || price <= 0) {
        priceInput.classList.add("error");
        alert('Пожалуйста, заполните  поле, оно не должны быть пустым или отрицательным');
        return
    }
    await request_for_redact(id,name,price);
    await load_serviceContent(document);
    modal.style.display = "none";
}


async function request_for_redact(id,name,price){
    const url = `http://localhost:8080/api/service/update/${id}`;
    const body = {
        method : 'PUT',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body: JSON.stringify({
            'name': name,
            'price': price
        })
    }
    const responce = await do_request(url,body);
    return responce;
}

async function get_service_info(servise_id) {
    const url = `http://localhost:8080/api/service/get/${servise_id}`;
    const body = {
        method : 'GET',
        headers : {
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    const responce = await do_request(url,body);
    return responce;
}

async function get_services() {
    const url = "http://localhost:8080/api/service/getAll";
    const body = 
    {
        method : 'GET',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }

    const responce = await do_request(url,body);
    return responce;
}