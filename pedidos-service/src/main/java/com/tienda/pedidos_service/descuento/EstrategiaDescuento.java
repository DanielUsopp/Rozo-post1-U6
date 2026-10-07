package com.tienda.pedidos_service.descuento;

import com.tienda.pedidos_service.validacion.ContextoPedido;

public interface EstrategiaDescuento {
    double calcular(ContextoPedido contexto);
}