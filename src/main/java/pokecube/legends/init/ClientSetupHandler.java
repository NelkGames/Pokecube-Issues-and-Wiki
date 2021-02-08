package pokecube.legends.init;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import pokecube.legends.PokecubeLegends;
import pokecube.legends.Reference;
import pokecube.legends.blocks.PlantBase;
import pokecube.legends.client.render.block.Raid;
import pokecube.legends.tileentity.RaidSpawn;
import thut.core.client.gui.ConfigGui;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = Reference.ID, value = Dist.CLIENT)
public class ClientSetupHandler
{
    static final Predicate<Material> notSolid = m -> m == Material.GLASS || m == Material.ICE ||
    		m == Material.PACKED_ICE || m == Material.LEAVES || m == Material.ANVIL;

    @SubscribeEvent
    public static void setupClient(final FMLClientSetupEvent event)
    {
    	for (final RegistryObject<Block> reg : PokecubeLegends.BLOCKS_TAB.getEntries())
        {
    		final Block b = reg.get();
            if (b instanceof PlantBase) RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
            boolean fullCube = true;
                for (final BlockState state : b.getStateContainer().getValidStates())
                {
                    final Material m = state.getMaterial();
                    if (ClientSetupHandler.notSolid.test(m))
                    {
                        fullCube = false;
                        break;
                    }
                    try
                    {
                        final VoxelShape s = state.getShape(null, BlockPos.ZERO);
                        if (s != VoxelShapes.fullCube())
                        {
                            fullCube = false;
                            break;
                        }
                    }
                    catch (final Exception e)
                    {
                        fullCube = false;
                        break;
                    }
                }
            if (!fullCube) RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
        }
    	
    	for (final RegistryObject<Block> reg : PokecubeLegends.DECORATION_TAB.getEntries())
        {
    		final Block b = reg.get();
            if (b instanceof PlantBase) RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
            boolean fullCube = true;
                for (final BlockState state : b.getStateContainer().getValidStates())
                {
                    final Material m = state.getMaterial();
                    if (ClientSetupHandler.notSolid.test(m))
                    {
                        fullCube = false;
                        break;
                    }
                    try
                    {
                        final VoxelShape s = state.getShape(null, BlockPos.ZERO);
                        if (s != VoxelShapes.fullCube())
                        {
                            fullCube = false;
                            break;
                        }
                    }
                    catch (final Exception e)
                    {
                        fullCube = false;
                        break;
                    }
                }
            if (!fullCube) RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
        }

        for (final RegistryObject<Block> reg : PokecubeLegends.BLOCKS.getEntries())
        {
            final Block b = reg.get();
            boolean fullCube = true;
                for (final BlockState state : b.getStateContainer().getValidStates())
                {
                    final Material m = state.getMaterial();
                    if (ClientSetupHandler.notSolid.test(m))
                    {
                        fullCube = false;
                        break;
                    }
                    try
                    {
                        final VoxelShape s = state.getShape(null, BlockPos.ZERO);
                        if (s != VoxelShapes.fullCube())
                        {
                            fullCube = false;
                            break;
                        }
                    }
                    catch (final Exception e)
                    {
                        fullCube = false;
                        break;
                    }
                }
            if (!fullCube) RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
        }

        // Renderer for raid spawn
        ClientRegistry.bindTileEntityRenderer(RaidSpawn.TYPE, Raid::new);

        // Register config gui
        ModList.get().getModContainerById(Reference.ID).ifPresent(c -> c.registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, parent) -> new ConfigGui(PokecubeLegends.config, parent)));
    }
}
