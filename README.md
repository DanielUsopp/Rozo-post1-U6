# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño
de Software — Sexto Semestre. Un único proyecto Spring Boot
(pedidos-service/) con dos partes: diagnóstico y refactorización de
un antipatrón combinado en GestorPedidos, y diagnóstico y corrección
de un segundo antipatrón introducido al hacer crecer el mismo
proyecto con tres campañas de descuento.

## Decisiones de diseño

### Parte 1 — GestorPedidos
**Antipatrón identificado:** God Object y Spaghetti Code combinados.
GestorPedidos.procesarPedido() mezclaba 6 responsabilidades (validación
de stock, validación de cliente/mora, cálculo de subtotal, cálculo de
descuento con hasta 3 niveles de anidamiento, persistencia vía JDBC
embebido y notificación) en un único método de más de 100 líneas,
dentro de una clase de 340 líneas en total.
**Patrón aplicado:** Chain of Responsibility para las validaciones
(dependencia real de orden y corte anticipado) y Strategy para el
cálculo de descuento por tipo de cliente (sin dependencia de orden).
Alternativa descartada: una lista de predicados booleanos para las
validaciones, sin corte anticipado real.

### Parte 2 — Crecimiento del proyecto
**Antipatrón identificado:** Golden Hammer. Las tres campañas de
descuento (Black Friday, Corporativo, Volumen) se implementaron como
eslabones adicionales de la cadena de validación existente, aunque
no tenían ninguna dependencia de orden entre sí ni necesidad de corte
anticipado — la propiedad que sí justificaba la cadena en
ValidadorStock y ValidadorCliente. Se reutilizó Chain of
Responsibility porque "ya funcionó" en la Parte 1, sin evaluar si
correspondía al nuevo problema.
**Patrón aplicado:** Strategy, extendiendo SelectorEstrategiaDescuento
con CalculadorDescuentoFinal. Los tres eslabones mal aplicados y el
campo descuentoCampana se eliminaron del código (no se comentaron,
para no dejar un Lava Flow) y su historial queda documentado
únicamente en los commits de este repositorio.

## Cómo ejecutar
```bash
$mvn spring-boot:run$ mvn test
```

### Herramientas utilizadas
Java 17, Spring Boot, Spring JDBC, Maven, H2 Database

VS Code / IntelliJ IDEA, Git, GitHub

### Conclusiones
La refactorización de este proyecto demostró que la calidad del software no depende únicamente de la separación inicial en capas, sino de mantener una correspondencia precisa entre el problema de negocio y el patrón de diseño elegido. Identificar y corregir el antipatrón de God Object y Spaghetti Code permitió transformar un orquestador acoplado en un sistema modular donde la validación, la persistencia y las promociones tienen responsabilidades únicas. Asimismo, el análisis del Golden Hammer evidenció el riesgo de forzar un patrón exitoso (Chain of Responsibility) en escenarios que no requieren un flujo secuencial ni corte anticipado. Finalmente, la eliminación completa del código obsoleto en lugar de dejarlo comentado garantizó un código limpio y libre de Lava Flow, confiando la trazabilidad histórica a las herramientas de control de versiones como Git.