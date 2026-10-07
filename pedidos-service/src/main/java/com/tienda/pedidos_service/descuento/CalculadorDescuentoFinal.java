package com.tienda.pedidos_service.descuento;

import com.tienda.pedidos_service.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CalculadorDescuentoFinal {

    private final SelectorEstrategiaDescuento selectorPorCliente;
    private final List<EstrategiaDescuento> campanas;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorPorCliente,
                                    DescuentoBlackFriday blackFriday,
                                    DescuentoCorporativo corporativo,
                                    DescuentoVolumen volumen) {
        this.selectorPorCliente = selectorPorCliente;
        this.campanas = List.of(blackFriday, corporativo, volumen);
    }

    public double calcular(ContextoPedido contexto) {
        double porTipoCliente = selectorPorCliente.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double porCampana = campanas.stream()
                .mapToDouble(estrategia -> estrategia.calcular(contexto))
                .max().orElse(0.0);
        return Math.max(porTipoCliente, porCampana);
    }
}