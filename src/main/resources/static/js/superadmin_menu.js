import { do_request } from "./fetch_request.js";

// Тестовые данные
let stations = [
    { id: 1, name: 'СТО "Центральный"', address: 'ул. Ленина, 45' },
    { id: 2, name: 'СТО "Северный"', address: 'пр. Независимости, 120' }
];

document.addEventListener('DOMContentLoaded', () => {
    renderStations();

    document.getElementById('openAddStation').addEventListener('click', () => openModal('addStationModal'));
    document.getElementById('openAssignAdmin').addEventListener('click', () => openModal('assignAdminModal'));

    document.getElementById('overlay').addEventListener('click', closeAllModals);
    document.querySelectorAll('.close-modal').forEach(btn => {
        btn.addEventListener('click', closeAllModals);
    });

    document.getElementById('addStationForm').addEventListener('submit', handleAddStation);
    document.getElementById('assignAdminForm').addEventListener('submit', handleAssignAdmin);
    document.getElementById('editStationForm').addEventListener('submit', handleEditStation);
});

function openModal(modalId) {
    document.getElementById('overlay').classList.add('active');
    document.getElementById(modalId).classList.add('active');
}

function closeAllModals() {
    document.getElementById('overlay').classList.remove('active');
    document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
}

function renderStations() {
    const grid = document.getElementById('stationsGrid');
    const countSpan = document.getElementById('stationsCount');
    const select = document.getElementById('stationSelect');

    grid.innerHTML = '';
    select.innerHTML = '<option value="" disabled selected>-- Выберите станцию --</option>';
    countSpan.textContent = stations.length;

    stations.forEach(station => {
        const card = document.createElement('div');
        card.className = 'station-card';
        card.innerHTML = `
            <div class="card-actions">
                <button class="icon-btn edit-btn" data-id="${station.id}"><i class="fa-regular fa-pen-to-square"></i></button>
                <button class="icon-btn delete-btn" data-id="${station.id}"><i class="fa-regular fa-trash-can"></i></button>
            </div>
            <div class="icon-top"><i class="fa-solid fa-location-dot"></i></div>
            <h3>${station.name}</h3>
            <p>${station.address}</p>
        `;
        grid.appendChild(card);

        card.querySelector('.edit-btn').addEventListener('click', () => prepareEdit(station.id));
        card.querySelector('.delete-btn').addEventListener('click', () => handleDelete(station.id));

        const option = new Option(station.name, station.id);
        select.add(option);
    });
}

function prepareEdit(id) {
    const s = stations.find(x => x.id === id);
    if (s) {
        document.getElementById('editStationId').value = s.id;
        document.getElementById('editStationName').value = s.name;
        document.getElementById('editStationAddress').value = s.address;
        openModal('editStationModal');
    }
}

async function handleAddStation(e) {
    e.preventDefault();
    alert('Станция добавлена (имитация)');
    closeAllModals();
}

async function handleEditStation(e) {
    e.preventDefault();
    const id = parseInt(document.getElementById('editStationId').value);
    const name = document.getElementById('editStationName').value;
    const address = document.getElementById('editStationAddress').value;

    const index = stations.findIndex(s => s.id === id);
    if (index !== -1) {
        stations[index] = { ...stations[index], name, address };
        renderStations();
    }
    closeAllModals();
}

function handleDelete(id) {
    if (confirm('Удалить эту станцию?')) {
        stations = stations.filter(s => s.id !== id);
        renderStations();
    }
}

async function handleAssignAdmin(e) {
    e.preventDefault();
    alert('Админ назначен (имитация)');
    closeAllModals();
}