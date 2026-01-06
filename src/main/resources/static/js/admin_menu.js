import { load_serviceContent } from "./admin_service_actions.js"
import {load_orderContent} from "./admin_order_actions.js"
import {load_userContent} from "./admin_user_actions.js"

document.querySelectorAll('input[name = "chapter"]').forEach(radio =>{
    radio.addEventListener("change",()=>loadContent(radio.value))
})

async function loadContent(value){
    if(value == "order"){
        await load_orderContent(document);
    }
    else if(value == "service"){
        await load_serviceContent(document);
    }
    else{
        await load_userContent(document);
    }
}

const checkedRadio = document.querySelector('input[name="chapter"]:checked');
if (checkedRadio) {
  await loadContent(checkedRadio.value);
}
