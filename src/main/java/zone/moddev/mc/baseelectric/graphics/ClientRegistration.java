package zone.moddev.mc.baseelectric.graphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import zone.moddev.mc.baseelectric.ElectricAdvantage;
import zone.moddev.mc.baseelectric.init.Entities;

/** Client-only renderer registration. */
@Mod.EventBusSubscriber(modid = ElectricAdvantage.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
