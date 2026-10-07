package com.tienda.pedidos_service.validacion;

public abstract class ValidadorPedido {
    private ValidadorPedido siguiente;

    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        ejecutar(contexto);
        if (!contexto.isRechazado() && siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void ejecutar(ContextoPedido contexto);
}