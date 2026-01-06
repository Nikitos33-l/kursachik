import {do_request} from "./fetch_request.js";

document.addEventListener('DOMContentLoaded',()=>{
    document.querySelectorAll("input").forEach(i=>i.value = "");
})
document.getElementById('login-tab').addEventListener('click', () => switchForm('login'));
document.getElementById('register-tab').addEventListener('click', () => switchForm('register'));
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('login_but').addEventListener('click',login_user);
    document.getElementById('register_but').addEventListener('click',register_user);
  });

function switchForm(formId) {
    document.querySelectorAll('.tab').forEach(tab => {
      tab.classList.remove('active');
    });
    const tabIndex = formId === 'login' ? 0 : 1;
    document.querySelectorAll('.tab')[tabIndex].classList.add('active');

    document.querySelectorAll('.form').forEach(form => {
      form.classList.remove('active');
    });
    const targetForm = document.getElementById(formId);
    targetForm.classList.add('active');

    targetForm.querySelectorAll('input').forEach(input => {
    input.value = '';
  });
}

async function register_user(event){
    event.preventDefault();
    const form = document.getElementById('register');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    const name = document.getElementById("register_name").value.trim();
    const email = document.getElementById("register_email").value.trim();
    const password = document.getElementById("register_password").value.trim();

    const url = "http://localhost:8080/api/auth/register";
    const body = {
        method: 'POST',
        headers: {
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body : JSON.stringify({
           'name' : name,
           'email' : email,
           'password' : password 
        })
    }
    const responce = await do_request(url,body);
    if(responce.ok){
        const data = await responce.json();
        const acesstoken = data.Acesstoken;
        const refreshtoken = data.Refreshtoken;
        localStorage.setItem("acesstoken",acesstoken);
        localStorage.setItem("refreshtoken",refreshtoken);
        window.location.assign("client_menu.html");
    }
    else if(responce.status == 409){
        alert("Этот email уже занят")
    }
    else{
        alert("Упс произошла ошибка")
    }
    
}

async function login_user(event) {
    event.preventDefault();
    const form = document.getElementById('login');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    const email = document.getElementById("login_email").value.trim();
    const password = document.getElementById("login_password").value.trim();
    const url = "http://localhost:8080/api/auth/login";
    const body = {
        method: 'POST',
        headers: {
            'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`,
            'Content-Type':'application/json'
        },
        body : JSON.stringify({
           'email' : email,
           'password' : password,
        })
    }
    const responce = await do_request(url,body);
    if(responce.ok){
        const data = await responce.json();
        const acesstoken = data.Acesstoken;
        const refreshtoken = data.Refreshtoken;
        localStorage.setItem("acesstoken",acesstoken);
        localStorage.setItem("refreshtoken",refreshtoken);
        const role = getRole(acesstoken);
        if(role == "ADMIN"){
            document.location.assign("admin_menu.html");
        }
        else if(role == "CLIENT"){
            document.location.assign("client_menu.html");
        }
        else{
            document.location.assign("worker_menu.html");
        }
    }
    else if(responce.status == 404){
        alert("Ваш аккаунт не найден, неверный логин или пароль");
    }
    else{
        alert("Упс произошла ошибка");
    }
}

function getRole(token){
    return JSON.parse(atob(token.split('.')[1])).role;
}
