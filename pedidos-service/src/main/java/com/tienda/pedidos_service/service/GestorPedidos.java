package com.tienda.pedidos_service.service;

import com.tienda.pedidos_service.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos_service.dto.ItemPedido;
import com.tienda.pedidos_service.dto.PedidoRequest;
import com.tienda.pedidos_service.dto.ResultadoPedido;
import com.tienda.pedidos_service.repository.PedidoRepository;
import com.tienda.pedidos_service.validacion.ContextoPedido;
import com.tienda.pedidos_service.validacion.ValidadorCliente;
import com.tienda.pedidos_service.validacion.ValidadorPedido;
import com.tienda.pedidos_service.validacion.ValidadorStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GestorPedidos {

    private final ValidadorPedido primerValidador;
    private final CalculadorDescuentoFinal calculadorDescuento;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;
    private final JdbcTemplate jdbcTemplate;

    public GestorPedidos(ValidadorStock stock,
                         ValidadorCliente cliente,
                         CalculadorDescuentoFinal calculadorDescuento,
                         PedidoRepository repository,
                         NotificacionPedidoService notificacion,
                         JdbcTemplate jdbcTemplate) {
        // La cadena recupera su propósito único: validar y aplicar corte anticipado
        stock.encadenar(cliente);
        this.primerValidador = stock;
        this.calculadorDescuento = calculadorDescuento;
        this.repository = repository;
        this.notificacion = notificacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        // 1. Cadena de Validaciones (2 eslabones estrictos: Stock -> Cliente)
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) {
            return ResultadoPedido.rechazado(contexto.getMotivoRechazo());
        }

        // 2. Cálculo del Subtotal
        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        // 3. Cálculo del Descuento Unificado mediante Strategy
        double descuento = calculadorDescuento.calcular(contexto);
        double impuesto = (subtotal - subtotal * descuento) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        // 4. Persistencia y Notificación
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