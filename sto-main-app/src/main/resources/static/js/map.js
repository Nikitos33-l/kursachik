import {do_request} from "./fetch_request.js"
ymaps.ready(init);

async function init() {
    let myMap = new ymaps.Map("map", {
        center: [53.904539, 27.561524],
        zoom: 12,
        controls: ['zoomControl', 'fullscreenControl']
    });
    const url = "http://localhost:8080/api/stations/findAll";
    const body = {
       method : "GET",
       headers:{
          'Autorization':`Bearer ${localStorage.getItem("acesstoken")}`
       }
    };
    const response = await do_request(url,body);
    const stations = await response.json();
    stations.forEach(station => {
                const placemark = new ymaps.Placemark(
                    [station.latitude, station.longitude],
                    {
                        balloonContentHeader: `<strong>${station.name}</strong>`,
                        balloonContentBody: `
                            <p>${station.address}</p>
                            <button class="select-btn" data-id="${station.id}"
                                    style="background: #2196F3; color: white; border: none; padding: 8px; cursor: pointer; border-radius: 4px;">
                                Выбрать это СТО
                            </button>
                        `
                    },
                    {
                        preset: 'islands#blueAutoIcon'
                    }
                );

                myMap.geoObjects.add(placemark);
    });
}