package uk.co.atty29.jsgzpm.dhd;

import dev.tauri.jsg.client.renderer.blockentity.dialhomedevice.DHDPegasusRendererState;
import dev.tauri.jsg.common.dialhomedevice.animation.DHDButtonsState;
import dev.tauri.jsg.common.dialhomedevice.manager.state.DHDAbstractStateManager;
import uk.co.atty29.jsgzpm.blockentity.AtlantisPegasusDHDBlockEntity;

/**
 * Pegasus button-state manager adapted to JSG-ZPM's own registered DHD block entity.
 * JSG's stock DHDPegasusStateManager is intentionally typed only to DHDPegasusBE,
 * so an addon-owned block entity needs the same small state-manager specialization.
 */
public final class AtlantisPegasusDHDStateManager
        extends DHDAbstractStateManager<AtlantisPegasusDHDBlockEntity, DHDPegasusRendererState> {

    public AtlantisPegasusDHDStateManager(AtlantisPegasusDHDBlockEntity dhd) {
        super(dhd);
    }

    @Override
    protected DHDButtonsState generateButtonsState() {
        return new DHDButtonsState(this, dhd.getSymbolType(), symbol -> {
            if (symbol.brb()) return "pegasus/dhd/dhd_bbb_";
            return "pegasus/dhd/dhd_button_light_";
        });
    }

    @Override
    protected DHDPegasusRendererState createRendererStateClient() {
        return new DHDPegasusRendererState();
    }
}
