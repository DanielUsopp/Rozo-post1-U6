package com.tienda.pedidos_service.validacion;

import org.springframework.stereotype.Component;

@Component
public class PromocionVolumen extends ValidadorPedido {

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        int totalUnidades = contexto.getRequest().getItems().stream()
                .mapToInt(item -> item.getCantidad()).sum();
        if (totalUnidades > 20) {
            contexto.aplicarDescuentoCampana(0.12);
        }
    }
}