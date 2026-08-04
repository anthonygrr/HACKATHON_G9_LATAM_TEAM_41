package smart.finance.ai.service;

import org.springframework.stereotype.Service;

@Service
public class ClasificadorServiceImpl implements ClasificadorService {

    @Override
    public Integer predecirTipoTransaccion(String descripcion) {
        if (descripcion == null) {
            return 2; // Default a GASTO
        }

        String desc = descripcion.toLowerCase();
        if (desc.contains("nomina") || desc.contains("pago") || desc.contains("ingreso") || desc.contains("salario")) {
            return 1; // ID 1 = INGRESO
        }
        return 2; // ID 2 = GASTO
    }
}
