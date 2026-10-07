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