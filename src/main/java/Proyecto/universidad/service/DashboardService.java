package Proyecto.universidad.service;

import Proyecto.universidad.model.Dashboard;
import Proyecto.universidad.repository.DashboardRepository;

public class DashboardService {

    private final DashboardRepository repository = new DashboardRepository();

    public Dashboard obtenerResumen() {
        return repository.obtenerResumen();
    }
}