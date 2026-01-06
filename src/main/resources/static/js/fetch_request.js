import {refresh_token} from "./auth.js"

export async function do_request(url,body) {
    console.log(body);
    const response = await fetch(url,body);
    if(response.ok){
        return response
    }
    else if(response.status == 401){
        await refresh_token();
        const headers = { ...body.headers };
        delete headers['Autorization'];
        headers['Autorization'] = `Bearer ${localStorage.getItem("acesstoken")}`;
    
        const newBody = {
            ...body,
            headers
        };
        const response2 = await fetch(url, newBody);
        return response2;
    }
    else{
        return response;
    }
}