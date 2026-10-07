package com.tienda.pedidos_service.service;

import com.tienda.pedidos_service.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos_service.dto.ItemPedido;
import com.tienda.pedidos_service.dto.PedidoRequest;
import com.tienda.pedidos_service.dto.ResultadoPedido;
import com.tienda.pedidos_service.repository.PedidoRepository;
import com.tienda.pedidos_service.validacion.ContextoPedido;
import com.tienda.pedidos_service.validacion.PromocionBlackFriday;
import com.tienda.pedidos_service.validacion.PromocionCorporativo;
import com.tienda.pedidos_service.validacion.PromocionVolumen;
import com.tienda.pedidos_service.validacion.ValidadorCliente;
import com.tienda.pedidos_service.validacion.ValidadorPedido;
import com.tienda.pedidos_service.validacion.ValidadorStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GestorPedidos {

    private final ValidadorPedido primerValidador;
    private final SelectorEstrategiaDescuento selector;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;
    private final JdbcTemplate jdbcTemplate;

    public GestorPedidos(ValidadorStock stock,
                         ValidadorCliente cliente,
                         PromocionBlackFriday blackFriday,
                         PromocionCorporativo corporativo,
                         PromocionVolumen volumen,
                         SelectorEstrategiaDescuento selector,
                         PedidoRepository repository,
                         NotificacionPedidoService notificacion,
                         JdbcTemplate jdbcTemplate) {
        // Se enganchan las promociones directamente en la cadena de validación
        stock.encadenar(cliente)
                .encadenar(blackFriday)
                .encadenar(corporativo)
                .encadenar(volumen);

        this.primerValidador = stock;
        this.selector = selector;
        this.repository = repository;
        this.notificacion = notificacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) {
            return ResultadoPedido.rechazado(contexto.getMotivoRechazo());
        }

        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        double descuentoTipoCliente = selector.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double descuento = Math.max(descuentoTipoCliente, contexto.getDescuentoCampana());
        double impuesto = (subtotal - subtotal * descuento) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);

        return ResultadoPedido.confirmado(pedidoId, total);
    }

    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;
        for (ItemPedido item : request.getItems()) {
            Double precioUnitario = jdbcTemplate.queryForObject(
                    "SELECT precio FROM productos WHERE id = ?", Double.class, item.getProductoId());
            if (precioUnitario != null) {
                subtotal += precioUnitario * item.getCantidad();
            }
        }
        return subtotal;
    }
}