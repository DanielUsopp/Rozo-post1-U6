package com.tienda.pedidos_service.validacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromocionBlackFriday extends ValidadorPedido {

    private final boolean campanaActiva;

    public PromocionBlackFriday(@Value("${promo.black-friday.activa:false}") boolean campanaActiva) {
        this.campanaActiva = campanaActiva;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        if (campanaActiva) {
            contexto.aplicarDescuentoCampana(0.25);
        }
    }
}