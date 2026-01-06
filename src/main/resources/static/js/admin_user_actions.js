import {do_request} from "./fetch_request.js"

let document

export async function load_userContent(document_page){
    document = document_page;
    const content = document.querySelector('.content');
    const template = document.querySelector(".user-content");
    const item = template.content.cloneNode(true);
    const add_user_button = item.querySelector(".add_user_button");
    const tbody = item.querySelector("tbody") 
    const responce = await get_users();
    const users = await responce.json();
    tbody.innerHTML = "";
    add_user_button.addEventListener('click',async ()=>await add_user())
    users.forEach(element =>{
        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${element.name}</td>
            <td>${element.email}</td>
            <td>${getRoleBadge(element.role)}</td>
            <td>
                <button class="action-edit" data-id="${element.id}">
                    <img src="../icons/change.png" alt="edit" style="width:17px;height:17px">
                </button>
                <button class="action-view" data-id="${element.id}">
                    <img src="../icons/trash.png" alt="delete" style="width:17px;height:17px">
                </button>
            </td>
        `;
    
        tbody.appendChild(row);
        const del_button = row.querySelector(".action-view");
        del_button.addEventListener('click',async ()=>{
            await delete_users(del_button.dataset.id);
            await load_userContent(document)
        },)
        const redact_user_but = row.querySelector(".action-edit");
        redact_user_but.addEventListener('click',async ()=>{
            await redact_users(redact_user_but.dataset.id);
        })
        
    })
    content.innerHTML = "";
    content.appendChild(item);
}

function getRoleBadge(role) {
    const roleMap = {
        'CLIENT': { label: 'Клиент', cls: 'role-client' },
        'WORKER': { label: 'Механик', cls: 'role-worker' },
        'ADMIN':  { label: 'Администратор', cls: 'role-admin' }
    };

    const { label, cls } = roleMap[role] || { label: role, cls: 'role-unknown' };
    return `<span class="role-badge ${cls}">${label}</span>`;
}

async function get_users() {
    const url = "http://localhost:8080/api/user/getAll";
    const body = {
        method : 'GET',
        headers : {
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    const responce = await do_request(url,body);
    return responce;
}

async function delete_users(id) {
    const url = `http://localhost:8080/api/user/delete/${id}`
    const body = {
        method : 'DELETE',
        headers : {
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    const responce = await do_request(url,body);
    return responce;
}

async function redact_users(id) {
    const responce = await get_user_info(id);
    const user_info = await responce.json();
    const modal = document.getElementById("user-modal");
    const nameInput = document.getElementById("user-modal-name");
    const emailInput = document.getElementById("user-modal-email");
    const cancelBtn = document.getElementById("user-modal-cancel-btn");
    const saveBtn = document.getElementById("user-modal-save-btn");

    nameInput.classList.remove("error");
    emailInput.classList.remove("error");
    nameInput.value = user_info.name;
    emailInput.value = user_info.email;
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.replaceWith(newSaveBtn);
    modal.style.display = "flex";

    cancelBtn.onclick = () => {
        modal.style.display = "none";
    };

    const handler = async()=>{ 
        const newName = nameInput.value;
        const newEmail = emailInput.value;
        if (newName.length < 2) {
            alert("Имя должно содержать минимум 2 символа!");
            nameInput.classList.add("error");
            return;
        }
    
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(newEmail)) {
            alert("Введите корректный email!");
            emailInput.classList.add("error");
            return;
        }
        await update_user(id,newName,newEmail);
        await load_userContent(document);
        modal.style.display = "none";
    }
    newSaveBtn.addEventListener('click',handler);
}

async function get_user_info(id) {
    const url = `http://localhost:8080/api/user/get/info/${id}`
    const body = {
        method:'GET',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
        }
    }
    const responce = await do_request(url,body);
    return responce;
}

async function update_user(id,name,email) {
    const url = `http://localhost:8080/api/user/update/${id}`;
    const body = {
        method:'PUT',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body:JSON.stringify({
            'name':name,
            'email':email
        })
    }

    const responce = await do_request(url,body);
    if(responce.status == 409){
        window.alert("Данный email уже занят");
    }
    return responce;
}

async function add_user() {
    const modal = document.getElementById("add-user-modal");
    const nameInput = document.getElementById("add-user-name");
    const emailInput = document.getElementById("add-user-email");
    const passwordInput = document.getElementById("add-user-password");
    const roleSelect = document.getElementById("add-user-role");
    const cancelBtn = document.getElementById("add-user-cancel-btn");
    const saveBtn = document.getElementById("add-user-save-btn");
    
    nameInput.value = "";
    emailInput.value = "";
    passwordInput.value = "";
    [nameInput, emailInput, passwordInput].forEach(el => el.classList.remove("input-error"));
    modal.style.display = "flex";
    const handler = async ()=>{
        const name = nameInput.value;
        const email = emailInput.value;
        const password = passwordInput.value;
        if (name.length < 2) {
            alert("Имя должно содержать минимум 2 символа!");
            nameInput.classList.add("error");
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            alert("Введите корректный email!");
            emailInput.classList.add("error");
            return;
        }

        if (password.length < 6) {
            alert("Пароль должен содержать минимум 6 символов!");
            passwordInput.classList.add("error");
            return;
        }
        await do_add_request(name,email,password,roleSelect.value);
        await load_userContent(document);
        modal.style.display = "none";
    }
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.replaceWith(newSaveBtn);
    newSaveBtn.addEventListener('click', handler);

    cancelBtn.addEventListener('click',()=>modal.style.display = "none");
}
async function do_add_request(name,email,password,role) {
    const url = "http://localhost:8080/api/user/add";
    const body = {
        method:'POST',
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body:JSON.stringify({
            'name':name,
            'email':email,
            'password':password,
            'role':role
        })
    }
    const responce = await do_request(url,body);
    if(responce.status == 409){
        window.alert("Данный email уже занят");
    }
    return responce;
}
