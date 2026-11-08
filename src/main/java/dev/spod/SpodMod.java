package dev.spod;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SpodMod.MOD_ID, dist = Dist.CLIENT)
public final class SpodMod {
    public static final String MOD_ID = "spod";

    public SpodMod(IEventBus modBus) {
        modBus.addListener(OverlayControls::registerKeys);
        NeoForge.EVENT_BUS.addListener(AutoReconnect::onTick);
        NeoForge.EVENT_BUS.addListener(AutoReconnect::onScreenInit);
        NeoForge.EVENT_BUS.addListener(AutoReconnect::onScreenRender);
        modBus.addListener(PlacementOrientation::registerKeys);
        NeoForge.EVENT_BUS.addListener(PlacementOrientation::onTick);
        NeoForge.EVENT_BUS.addListener(PlacementOrientation::onScroll);
        NeoForge.EVENT_BUS.addListener(PlacementOrientation::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(PlacementOrientation::onRenderGui);
        NeoForge.EVENT_BUS.addListener(OverlayControls::onTick);
        NeoForge.EVENT_BUS.addListener(HiveData::onTick);
        NeoForge.EVENT_BUS.addListener(HiveCounter::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(SpawnOverlay::onTick);
        NeoForge.EVENT_BUS.addListener(SpawnOverlay::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(ClickController::registerCommands);
        NeoForge.EVENT_BUS.addListener(ScrambleCommand::registerCommands);
        NeoForge.EVENT_BUS.addListener(ClickController::onTick);
        NeoForge.EVENT_BUS.addListener(MegaSiteFinder::onTick);
        NeoForge.EVENT_BUS.addListener(MiningController::registerCommands);
        NeoForge.EVENT_BUS.addListener(MiningController::onTick);
        NeoForge.EVENT_BUS.addListener(MiningController::onInput);
        NeoForge.EVENT_BUS.addListener(MiningController::onFloorTick);
    }
}
