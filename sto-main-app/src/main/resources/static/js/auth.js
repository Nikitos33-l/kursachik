const path_for_refresh = "http://localhost:8080/api/token/refresh";
export async function refresh_token() {
    const response = await fetch(path_for_refresh,{
        method : "POST",
        credentials : "include",
        headers:{
            'Autorization':`Bearer ${localStorage.getItem("refreshtoken")}`
        }
    })
    console.log("jjjjjjjjjj");
    if(response.ok){
        console.log("eeeeeeeeeeeeeeeeee");
        const token = await response.json();
        console.log(token);
        localStorage.setItem("acesstoken",token.Acesstoken)
    }
}