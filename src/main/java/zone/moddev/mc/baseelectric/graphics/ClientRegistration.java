package zone.moddev.mc.baseelectric.graphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Entities;

/** Client-only renderer registration. */
@Mod.EventBusSubscriber(modid = ElectricAdvantage.MODID, value = Dist.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Entities.ELECTRIC_DRILL, LaserDrillRenderer::new);
        event.registerBlockEntityRenderer(Entities.LASER_TURRET, LaserTurretRenderer::new);
        event.registerEntityRenderer(Entities.HYDROTURBINE, HydroturbineRenderer::new);
    }

}
