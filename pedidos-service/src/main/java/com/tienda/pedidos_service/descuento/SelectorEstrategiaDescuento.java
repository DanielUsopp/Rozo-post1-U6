package com.tienda.pedidos_service.descuento;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SelectorEstrategiaDescuento {
    private final Map<String, EstrategiaDescuento> estrategias;

    public SelectorEstrategiaDescuento(DescuentoVip vip, DescuentoFrecuente frecuente, DescuentoEstandar estandar) {
        this.estrategias = Map.of("VIP", vip, "FRECUENTE", frecuente, "ESTANDAR", estandar);
    }

    public EstrategiaDescuento seleccionar(String tipoCliente) {
        return estrategias.getOrDefault(tipoCliente, estrategias.get("ESTANDAR"));
    }
}