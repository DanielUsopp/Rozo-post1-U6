# Paso 3: Diagnóstico de Antipatrones y Problemas de Diseño

## 1. Antipatrón Principal: Big Ball of Mud / God Class (Clase Monolítica)

La clase `GestorPedidos.java` asume múltiples responsabilidades operativas e infraestructurales en un único bloque de ejecución. Violando el **Principio de Responsabilidad Única (SRP)**, acumula **6 razones distintas para cambiar**:

1. **Validación de Stock:** Si cambia la regla de verificación de inventario o si la consulta pasa a un microservicio de stock, hay que modificar las líneas 31 a 43.
2. **Validación de Cliente y Mora:** Si cambian las políticas de mora, tipos de cliente o la regla de horario de corte (`LocalTime.of(20, 0)`), hay que modificar las líneas 46 a 64.
3. **Cálculo de Precios y Subtotal:** Si la consulta de precios evoluciona hacia un esquema de tarifas dinámicas o impuestos variables, hay que modificar las líneas 67 a 72 y 89 a 90.
4. **Cálculo de Descuentos:** Si se agregan o modifican categorías de clientes o umbrales de compra, hay que modificar las líneas 75 a 87.
5. **Persistencia Directa:** Si cambia la estructura de las tablas (`pedidos`, `detalle_pedido`, `inventario`) o el motor de base de datos, hay que modificar las líneas 93 a 108.
6. **Notificación y Formato de Correo:** Si se modifica el canal de comunicación o el formato/plantilla del mensaje, hay que modificar las líneas 111 a 124.

---

## 2. Antipatrón Secundario: Spaghetti Code / Anidamiento Excesivo

El código presenta estructuras de control profundamente anidadas que incrementan la complejidad ciclomática y dificultan la lectura y el mantenimiento:

* **Anidamiento en Validación de Mora (hasta 3 niveles):**
    * Nivel 1: `else if (tipoCliente.equals("MOROSO"))` (Línea 50)
    * Nivel 2: `if (deudaPendiente != null && deudaPendiente > 0)` (Línea 54)
    * Nivel 3: `if (ahora.isBefore(LocalTime.of(20, 0)))` (Línea 56)
* **Anidamiento en Cálculo de Descuentos (hasta 3 niveles):**
    * Nivel 1: `if (tipoCliente.equals("VIP"))` (Línea 75)
    * Nivel 2: `if (subtotal > 1_000_000)` vs `else if (subtotal > 500_000)` (Líneas 76-78)
    * Nivel 3 (Ruta Frecuente): `else if (tipoCliente.equals("FRECUENTE"))` $\rightarrow$ `if (pedidosPrevios != null && pedidosPrevios > 3)` (Líneas 81-84)

---

## 3. Mezcla de Niveles de Abstracción

El método `procesarPedido` actúa simultáneamente en múltiples capas conceptuales del sistema en una misma secuencia continua de líneas:

* **Nivel de Infraestructura / SQL Directo:** Consultas SQL nativas como `SELECT stock FROM inventario WHERE producto_id = ?` (Líneas 38-39) y `CALL IDENTITY()` (Línea 100).
* **Nivel de Dominio / Reglas de Negocio:** Lógica de elegibilidad de descuentos basados en el volumen de compra y tipo de cliente (Líneas 75-87).
* **Nivel de Formato y Presentación:** Construcción de cadenas de texto en formato plano para el cuerpo del correo mediante `StringBuilder` (Líneas 113-120).

---

## 4. Impacto en la Mantenibilidad (Análisis de Cambio)

**Escenario:** Se requiere agregar un nuevo tipo de cliente denominado `CORPORATIVO` con un esquema de descuento del 12% fijo si la compra supera los $2,000,000.

* **Efecto en el código actual:**
    1. Se deben modificar directamente las líneas 75 a 87 dentro del método `procesarPedido` en `GestorPedidos.java`.
    2. Habría que agregar una nueva rama condicional `else if (tipoCliente.equals("CORPORATIVO"))` entrelazada dentro del flujo principal.
    3. **Riesgo:** Al no estar aislada la lógica de descuentos, cualquier modificación accidental en este bloque corre el riesgo de alterar la persistencia o el flujo de notificaciones, exigiendo re-probar el método completo de 340 líneas.


## Justificación de Decisiones de Arquitectura

### 1. Validación como Chain of Responsibility vs. Lista de Predicados
Se eligió **Chain of Responsibility** para la secuencia de validaciones debido a la **dependencia real de orden** y la necesidad de un **corte anticipado (*short-circuiting*)**:
* **Razón:** Si `ValidadorStock` rechaza el pedido, `ValidadorCliente` ni siquiera debe ejecutarse.
* **Alternativa considerada:** Método `validarTodo()` con una lista de `Predicate<ContextoPedido>`.
* **Desventaja de la alternativa:** Evalúa todos los predicados aunque el primero haya fallado y no permite que un validador decida romper la cadena de ejecución directamente.

---

### 2. Descuento como Strategy vs. Eslabón de la Cadena
Se eligió **Strategy** en lugar de agregar el cálculo de descuento a la cadena de validación porque las reglas de descuento **no tienen dependencia de orden ni necesitan cortar el flujo**:
* **Razón:** Siempre se aplica exactamente una regla, determinada de forma mutuamente excluyente por el tipo de cliente.
* **Alternativa considerada:** Añadir un eslabón `ValidadorDescuento` dentro de la cadena.
* **Desventaja de la alternativa:** Obligaría a crear mecanismos artificiales para evitar la sobreescritura de descuentos o detener la cadena cuando el pedido es válido. Un mapa de selección directa (`SelectorEstrategiaDescuento`) resuelve el problema con menor indirección y eliminando condicionales.

## Diagnóstico de la Parte 2: Antipatrón Golden Hammer (Martillo de Oro)

### 1. Definición del Antipatrón
El antipatrón **Golden Hammer** ocurre cuando una herramienta, solución o patrón de diseño que funcionó con éxito para un problema específico (en este caso, *Chain of Responsibility* para validaciones secuenciales) se aplica indiscriminadamente a problemas conceptualmente distintos, ignorando si la abstracción sigue siendo adecuada.

### 2. Evidencia Concreta en el Código

1. **Violación del Propósito del Patrón (*Chain of Responsibility*):**
  * El propósito de `ValidadorPedido` es la verificación y posible rechazo (*short-circuit*) del pedido.
  * Las clases `PromocionBlackFriday`, `PromocionCorporativo` y `PromocionVolumen` extienden `ValidadorPedido`, pero **ninguna de ellas valida ni rechaza jamás un pedido**. Solo modifican el estado mutable del `ContextoPedido` escribiendo un descuento.

2. **Efectos Secundarios Ocultos y Violación de Responsabilidad:**
  * Al encadenar `stock.encadenar(cliente).encadenar(blackFriday)...`, el método `validar()` pasa a calcular descuentos de forma implícita. La fase de "validación" deja de ser pura y genera mutaciones laterales.

3. **Incoherencia en el Flujo de Ejecución y Dependencias Incompletas:**
  * `PromocionVolumen` necesita evaluar las unidades totales del pedido. Sin embargo, como se ejecuta dentro de la fase de validación (antes de que el orquestador calcule e inyecte el subtotal definitivo), se ve obligada a recalcular de forma independiente la cantidad total desde el `PedidoRequest`.

4. **Fragmentación y Duplicidad en el Cálculo de Descuentos:**
  * El descuento total del pedido queda dividido en dos mecanismos incoherentes:
    1. Un esquema basado en el patrón **Strategy** (`SelectorEstrategiaDescuento`) para descuentos por tipo de cliente.
    2. Un esquema mutador basado en **Chain of Responsibility** (`contexto.getDescuentoCampana()`).
  * El orquestador termina usando un parche explícito: `Math.max(descuentoTipoCliente, contexto.getDescuentoCampana())`, mezclando dos abstracciones totalmente distintas para resolver una misma regla de negocio.

# Paso 6: Diagnóstico del Antipatrón en la Parte 2 (Golden Hammer)

## 1. Identificación del Antipatrón: Golden Hammer (Martillo de Oro)

El problema presente en la Parte 2 no es un *God Object* ni *Spaghetti Code*, ya que el código está modularizado en clases independientes y no existen condicionales profundamente anidados. El problema es de **diseño arquitectónico**: se aplicó el antipatrón **Golden Hammer**, que consiste en reutilizar una solución o patrón que funcionó con éxito en el pasado (*Chain of Responsibility*) para resolver un problema de naturaleza completamente distinta, sin evaluar si la abstracción seguía siendo adecuada.

---

## 2. Respuestas a las Preguntas Guía con Evidencia Concreta

### 1. Dependencia de Orden y Corte Anticipado
* **Pregunta:** ¿Las tres clases promocionales tienen alguna dependencia de orden entre sí o respecto a `ValidadorStock` y `ValidadorCliente`?
* **Análisis y Evidencia:**
  En las validaciones reales existe una dependencia estricta: `ValidadorStock` **debe** ejecutarse antes que `ValidadorCliente` porque si no hay existencias en el inventario, no tiene sentido consultar la base de datos para verificar la mora del cliente.
  Por el contrario, ejecutar `PromocionVolumen` antes o después de `PromocionCorporativo` o `PromocionBlackFriday` **no altera en absoluto el resultado final**. Ninguna de las tres promociones necesita cortar el flujo de ejecución (*short-circuit*), propiedad fundamental que justificaba el uso de la cadena.

### 2. Violación del Contrato Original y la Abstracción
* **Pregunta:** ¿Por qué clases que extienden de `ValidadorPedido` nunca rechazan un pedido y solo escriben en un campo compartido?
* **Análisis y Evidencia:**
  El contrato abstracto de `ValidadorPedido` define la responsabilidad de *"decidir si el pedido continúa o se rechaza"*. Sin embargo:
  * `PromocionBlackFriday` (líneas 15-20)
  * `PromocionCorporativo` (líneas 12-18)
  * `PromocionVolumen` (líneas 8-13)

  **Nunca invocan `contexto.rechazar(...)`**. Su única función es modificar el estado mutable de `ContextoPedido` llamando a `aplicarDescuentoCampana(...)`. Se forzó la herencia de `ValidadorPedido` simplemente porque la infraestructura de encadenamiento ya estaba construida.

### 3. Rigidez frente a Cambios en las Reglas de Combinación
* **Pregunta:** ¿Qué pasaría si las campañas necesitaran combinarse (ej. sumar porcentajes) en lugar de competir por el máximo?
* **Análisis y Evidencia:**
  El mecanismo actual acopla la regla de combinación dentro de un método mutador en la clase de contexto:
  ```java
  public void aplicarDescuentoCampana(double valor) {
      if (valor > this.descuentoCampana) this.descuentoCampana = valor; // el mayor descuento gana
  }
 
### 4.Causa Raíz del Error de Diseño
* **Pregunta:** ¿Se eligió esta solución por ser la más adecuada o porque "ya existía y funcionó la última vez"?
* **Análisis y Evidencia:**
La solución se eligió únicamente porque la Chain of Responsibility resolvió el problema de validaciones en la Parte 1. Se "engancharon" tres eslabones más (`stock.encadenar(cliente).encadenar(blackFriday).encadenar(corporativo).encadenar(volumen)`) aprovechando que los eslabones "ya sabían cómo conectarse entre sí", ignorando que el cálculo de promociones es una regla de selección de tarifas (ideal para Strategy) y no una secuencia de validación previa al procesamiento.