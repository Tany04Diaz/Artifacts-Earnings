![alt text](EARNINGSLONG.png "logo")
# Artifacts Earnings
artefactos para servidores Spigot 1.21.x que añaden mecánicas útiles y configurables para economía, 
automatización y gameplay: una Harvester Hoe (cosecha inteligente), 
Sell Sticks (venta de cofres) y Crop Growth Totems (aceleradores de crecimiento). 
Está pensada para ser modular, configurable por admins y fácil de integrar en plugins existentes.
# Artefactos
## Harvester Hoe
- Función: azada que cosecha y replanta automáticamente cultivos (wheat, carrots, potatoes, beetroot, sugar cane).
- Modos: COLLECT (recoge items) y SELL (vende según precios y deposita en Economy). El modo se alterna solo para la Harvester con Shift + Right-Click.
- Usos: contador persistente en PDC (artifact_uses). Cada acción de cosecha consume 1 uso. Al llegar a 0 la herramienta se elimina del inventario.
- Integraciones: usa un servicio de precios configurable (PriceService) y Vault Economy para depositar dinero. Si faltan precios o economy, hace fallback y entrega items.
- Feedback visual: displayName y lore muestran usos; mensajes configurables y sonidos para eventos clave.
- Puntos importantes: debounce para evitar toggles dobles; sincronización estricta entre ItemMeta y la ranura del jugador para evitar desincronías.
## Sell Stick
- Función: sticks que venden el contenido de cofres al hacer Right-Click sobre un contenedor.
- Variantes: 10, 25, 50 usos (SellStick10 / SellStick25 / SellStick50).
- Usos: cada venta de un cofre decrementa 1 uso; al llegar a 0 la vara desaparece.
- Comportamiento: recorre el inventario del contenedor, calcula total según la tabla prices: en config y deposita con Vault. Los stacks sin precio quedan sin vender (puedes configurarlo).
  Si Economy no está disponible, la venta se desactiva y el jugador recibe un aviso.
- Puntos importantes: seguridad al modificar inventarios de cofres (trabaja por índices), mensajes configurables y sonidos de feedback.
## Crop Growth Totem
- Función: bloque consumible/colocable que acelera el crecimiento de cultivos en un radio alrededor. Ideal como bloque decorativo con utilidad práctica.
- Tiers: tres niveles con radios diferentes y efectos escalados:
- Tier 1: radio ≈ 4
- Tier 2: radio ≈ 10
- Tier 3: radio ≈ 25
- Mecánica: tarea periódica aplica probabilidades/multiplicadores para avanzar la edad de cultivos Ageable (wheat, carrots, potatoes, beetroot, sweet_berry_bush),
  simular crecimiento de sugar cane y cactus, y emitir partículas suaves. No fuerza ticks del servidor; simula crecimientos con control para evitar picos.
- Configuración: radios, multiplicadores y probabilidades por tier se ajustan en config; whitelist de cultivos configurable.
- Persistencia: el item Totem lleva el tier en PDC; al colocarlo el listener registra la ubicación; al romperlo se elimina el registro. (Opcional: persistencia entre reinicios en archivo YAML).
- Consideraciones de rendimiento: radios grandes requieren batching, throttling y tuning del intervalo; el plugin ofrece opciones para ajustar frecuencia y alcance.
  # Config
  **prices:
  WHEAT: 0.5
  POTATO: 0.2
  CARROT: 0.25
  SUGAR_CANE: 0.15
  **
  **
  harvester:
  displayName: "&6Harvester Hoe &7- &e%uses% usos"
  uses_default: 100
  messages:
    mode_sell: "&eModo: VENDER"
    mode_collect: "&eModo: RECOLECTAR"

  **
**
sellstick:
  displayName: "&6SellStick &7- &e%uses% usos"
  messages:
    sold: "&aVendiste %amount% items del cofre por %money%."

**
**
totem:
  material: STRIPPED_OAK_LOG
  pdc_key: crop_totem_tier
  apply_interval_ticks: 40
  tiers:
    1:
      radius: 4
      multiplier: 1.5
      extraChance: 0.25
    2:
      radius: 10
      multiplier: 2.0
      extraChance: 0.45
    3:
      radius: 25
      multiplier: 3.0
      extraChance: 0.75
crop_whitelist:
  - WHEAT
  - POTATOES
  - CARROTS
  - BEETROOT
  - SUGAR_CANE
  - CACTUS

**
