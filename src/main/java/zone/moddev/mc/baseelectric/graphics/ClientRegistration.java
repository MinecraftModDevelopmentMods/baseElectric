package zone.moddev.mc.baseelectric.graphics;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Entities;

/** Client-only renderer registration. */
@EventBusSubscriber(modid = ElectricAdvantage.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Entities.ELECTRIC_DRILL, LaserDrillRenderer::new);
        event.registerBlockEntityRenderer(Entities.LASER_TURRET, LaserTurretRenderer::new);
        event.registerEntityRenderer(Entities.HYDROTURBINE, HydroturbineRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ElectricRenderTypes.UNSHADED,
                DefaultVertexFormat.NEW_ENTITY), ElectricRenderTypes::setUnshadedShader);
    }
}
