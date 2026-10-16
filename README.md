# AE Compendium

One jar for the AE-side utility mods of the modpack (Minecraft 26.1.2, NeoForge). Each module is the fleet fork of
the original mod and keeps its own mod id, so item ids, tags, configs and compat with other mods stay unchanged.

| Module (mod id) | Source | License | Status |
|---|---|---|---|
| `constructionstick` | Mrbysco/ConstructionSticks (fork `ae-stack/core/ConstructionSticks`) | MIT | in |
| `appliedsticks` | benbenlaw/Applied-Construction-Sticks (fork `ae-stack/compendium-sources/…`) | MIT | in |
| `polymorph_plus` | Aminoquiz/polymorph_plus 26.1.2 (fork of Polymorph by Illusive Soulworks) — **jar module** (`ae-stack/core/polymorph_plus`) | LGPL-3.0-or-later | in |
| `polyeng` | 62832/PolymorphicEnergistics, ported to 26.1 (`ae-stack/compendium-sources/PolymorphicEnergistics`) | LGPL-3.0 | in |
| `buildinggadgets2` | Direwolf20-MC/BuildingGadgets2 branch `21.6` (= 26.1.2) (`ae-stack/core/BuildingGadgets2-26`); its AE2 link re-enabled | MIT | in |
| `ae2wtlib` (+ nested `ae2wtlib_api`) | Mari023/AE2WirelessTerminalLibrary tag 26.1.1-beta — **jar module** (`ae-stack/addons/AE2WTLib`, built as 26.1.1-beta.fleet) | MIT | in |
| `ae_compendium` | own code: **Order from AE** in the BG2 material list (crafts missing materials via the bound AE network) | MIT | in |

GadgetsmeNetwork (AGPL-3.0) is **not** used: owner decision 2026-10-10 — the feature is implemented independently in
`ae_compendium` (from its public description only).

### Rules for modules
- Source modules: compiled from the fork trees (`modules` in build.gradle); jar modules: built by their own build,
  classes/resources merged (`jarModules`).
- Every `@EventBusSubscriber` must name its `modid` — in a multi-mod jar an annotation without it is attached to every
  mod of the file (double registration crash, seen with Construction Sticks' key category).
- Access transformers are renamed to `META-INF/<modid>_at.cfg` at build time (several modules use the default path).
- Each module's `neoforge.mods.toml` is ignored; its `[[mods]]`, `[[mixins]]`, `[[accessTransformers]]` and
  dependencies go into ours.

Build: `./gradlew build` (sibling fork trees must be checked out next to this repo, as in `/root/home/forks`).

## Towards one pack mod

Owner's goal (2026-10-10): fewer files. Every Compendium (AE, EMI, MI, Create, Culinary, Colonial…) is built the same
way — a module list + one `neoforge.mods.toml` — so once they are ready they can be folded into a single pack jar by
concatenating the module lists and `[[mods]]` blocks. Keep module mod ids unique and resources namespaced per mod id.
