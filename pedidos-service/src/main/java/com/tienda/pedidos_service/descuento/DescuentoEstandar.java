package com.tienda.pedidos_service.descuento;

import com.tienda.pedidos_service.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

@Component
public class DescuentoEstandar implements EstrategiaDescuento {
    @Override
    public double calcular(ContextoPedido contexto) {
        return 0.0;
    }
}