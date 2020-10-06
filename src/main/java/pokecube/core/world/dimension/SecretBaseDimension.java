package pokecube.core.world.dimension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.world.RegisterDimensionsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pokecube.core.PokecubeCore;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.utils.PokecubeSerializer;
import thut.api.entity.ThutTeleporter;
import thut.api.entity.ThutTeleporter.TeleDest;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;

public class SecretBaseDimension extends ModDimension
{
    public static void sendToBase(final ServerPlayerEntity player, final UUID baseOwner)
    {
        final RegistryKey<World> targetDim = SecretBaseDimension.TYPE;
        final BlockPos pos = SecretBaseDimension.getSecretBaseLoc(baseOwner, player.getServer(), targetDim);
        final Vector3 v = Vector3.getNewVector().set(pos).addTo(0.5, 0, 0.5);
        ThutTeleporter.transferTo(player, new TeleDest().setLoc(GlobalPos.of(targetDim, pos), v), true);
        player.sendMessage(new TranslationTextComponent("pokecube.secretbase.enter"), Util.DUMMY_UUID);
    }

    public static void sendToExit(final ServerPlayerEntity player, final UUID baseOwner)
    {
        final RegistryKey<World> targetDim = World.OVERWORLD;
        final BlockPos pos = SecretBaseDimension.getSecretBaseLoc(baseOwner, player.getServer(), targetDim);
        final Vector3 v = Vector3.getNewVector().set(pos).addTo(0.5, 0, 0.5);
        ThutTeleporter.transferTo(player, new TeleDest().setLoc(GlobalPos.of(targetDim, pos), v), true);
        player.sendMessage(new TranslationTextComponent("pokecube.secretbase.exit"), Util.DUMMY_UUID);
    }

    public static void setSecretBasePoint(final ServerPlayerEntity player, final BlockPos pos, final RegistryKey<World> dim)
    {
        final CompoundNBT tag = PokecubePlayerDataHandler.getCustomDataTag(player);
        final CompoundNBT exit = new CompoundNBT();
        exit.putInt("x", pos.getX());
        exit.putInt("y", pos.getY());
        exit.putInt("z", pos.getZ());
        if (dim == SecretBaseDimension.TYPE) tag.put("secret_base_internal", exit);
        else
        {
            if (tag.contains("secret_base_exit"))
            {
                final CompoundNBT exito = tag.getCompound("secret_base_exit");
                final Vector4 old = new Vector4(exito.getInt("x"), exito.getInt("y"), exito.getInt("z"), dim.getId());
                PokecubeSerializer.getInstance().bases.removeIf(c -> old.withinDistance(0.25f, old));
            }
            tag.put("secret_base_exit", exit);
            PokecubeSerializer.getInstance().bases.add(GlobalPos.of(dim, pos));
        }
    }

    public static ChunkPos getFromIndex(final int index)
    {
        final int scale = 10;
        int x = index % (16 << scale) - (16 << scale) / 2;
        int z = index / (16 << scale) - (16 << scale) / 2;
        x *= 16;
        z *= 16;
        return new ChunkPos(x, z);
    }

    public static BlockPos getSecretBaseLoc(final UUID player, final MinecraftServer server, final RegistryKey<World> dim)
    {
        final CompoundNBT tag = PokecubePlayerDataHandler.getCustomDataTag(player.toString());
        if (dim == SecretBaseDimension.TYPE)
        {
            if (tag.contains("secret_base_internal"))
            {
                final CompoundNBT exit = tag.getCompound("secret_base_internal");
                return new BlockPos(exit.getInt("x"), exit.getInt("y"), exit.getInt("z"));
            }
            int index;
            if (!tag.contains("secret_base_index"))
            {
                index = PokecubeSerializer.getInstance().customData.getInt("next_base_index");
                tag.putInt("secret_base_index", index);
                PokecubeSerializer.getInstance().customData.putInt("next_base_index", index + 1);
            }
            else index = tag.getInt("secret_base_index");
            final ChunkPos chunk = SecretBaseDimension.getFromIndex(index);
            return new BlockPos((chunk.x << 4) + 8, 64, (chunk.z << 4) + 8);
        }
        else if (!tag.contains("secret_base_exit")) return server.getWorld(dim).getSpawnPoint();
        else
        {
            final CompoundNBT exit = tag.getCompound("secret_base_exit");
            return new BlockPos(exit.getInt("x"), exit.getInt("y"), exit.getInt("z"));
        }
    }

    public static class SecretBiome extends Biome
    {
        public SecretBiome()
        {
            super(new Biome.Builder().surfaceBuilder(SurfaceBuilder.DEFAULT, SurfaceBuilder.GRASS_DIRT_GRAVEL_CONFIG)
                    .precipitation(Biome.RainType.RAIN).category(Biome.Category.PLAINS).depth(0.125F).scale(0.05F)
                    .temperature(0.8F).downfall(0.4F).waterColor(4159204).waterFogColor(329011).parent((String) null));
            this.setRegistryName(PokecubeCore.MODID, "secret_base");
        }

    }

    public static class SecretChunkGen extends ChunkGenerator<SecretSettings>
    {

        public SecretChunkGen(final IWorld w, final BiomeProvider bp, final SecretSettings gs)
        {
            super(w, bp, gs);
        }

        @Override
        public int func_222529_a(final int arg0, final int arg1, final Type hmtype)
        {
            return 15;
        }

        @Override
        public void generateSurface(final WorldGenRegion p_225551_1_, final IChunk p_225551_2_)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public int getGroundHeight()
        {
            return 64;
        }

        @Override
        public void makeBase(final IWorld world, final IChunk chunk)
        {
            final ChunkPos pos = chunk.getPos();
            if (pos.x % 16 == 0 && pos.z % 16 == 0)
            {
                final BlockPos.Mutable blockpos$mutableblockpos = new BlockPos.Mutable();
                final Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
                final Heightmap heightmap1 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
                BlockState state = Blocks.STONE.getDefaultState();
                for (int i = 58; i < 64; ++i)
                    for (int j = 4; j < 12; ++j)
                        for (int k = 4; k < 12; ++k)
                        {
                            chunk.setBlockState(blockpos$mutableblockpos.setPos(j, i, k), state, false);
                            heightmap.update(j, i, k, state);
                            heightmap1.update(j, i, k, state);
                        }
                state = Blocks.BARRIER.getDefaultState();
                for (int j = 0; j < 16; ++j)
                    for (int k = 0; k < 16; ++k)
                    {
                        chunk.setBlockState(blockpos$mutableblockpos.setPos(j, 0, k), state, false);
                        chunk.setBlockState(blockpos$mutableblockpos.setPos(j, world.getMaxHeight() - 1, k), state,
                                false);
                        chunk.setBlockState(blockpos$mutableblockpos.setPos(j, world.getMaxHeight() - 2, k), state,
                                false);
                        chunk.setBlockState(blockpos$mutableblockpos.setPos(j, world.getMaxHeight() - 3, k), state,
                                false);
                        chunk.setBlockState(blockpos$mutableblockpos.setPos(j, world.getMaxHeight() - 4, k), state,
                                false);
                        chunk.setBlockState(blockpos$mutableblockpos.setPos(j, world.getMaxHeight() - 5, k), state,
                                false);
                    }
            }
            else
            {
                final BlockPos.Mutable blockpos$mutableblockpos = new BlockPos.Mutable();
                final BlockState state = Blocks.BARRIER.getDefaultState();
                for (int i = 0; i < world.getMaxHeight(); ++i)
                    for (int j = 0; j < 16; ++j)
                        for (int k = 0; k < 16; ++k)
                            chunk.setBlockState(blockpos$mutableblockpos.setPos(j, i, k), state, false);
            }
        }

    }

    public static class SecretDimension extends OverworldDimension
    {

        public SecretDimension(final World worldIn, final DimensionType typeIn)
        {
            super(worldIn, typeIn);
        }

        @Override
        public ChunkGenerator<?> createChunkGenerator()
        {
            return new SecretChunkGen(this.getWorld(), new SecretProvider(), new SecretSettings());
        }

        @Override
        public boolean canRespawnHere()
        {
            return false;
        }
    }

    public static class SecretProvider extends BiomeProvider
    {
        private static final Set<Biome> BIOMES   = Sets.newHashSet(SecretBaseDimension.BIOME);
        Set<BlockState>                 blocks   = Sets.newHashSet(Blocks.STONE.getDefaultState());
        final Set<Biome>                biomes;
        Biome[]                         biomeArr = new Biome[256];

        public SecretProvider()
        {
            super(SecretProvider.BIOMES);
            this.biomes = SecretProvider.BIOMES;
        }

        @Override
        public Set<Biome> getBiomes(final int p_225530_1_, final int p_225530_2_, final int p_225530_3_,
                final int p_225530_4_)
        {
            return SecretProvider.BIOMES;
        }

        @Override
        public Set<BlockState> getSurfaceBlocks()
        {
            return this.blocks;
        }

        @Override
        public boolean hasStructure(final Structure<?> arg0)
        {
            return false;
        }

        @Override
        public Biome getNoiseBiome(final int x, final int y, final int z)
        {
            // TODO Auto-generated method stub
            return SecretBaseDimension.BIOME;
        }

    }

    public static class SecretSettings extends GenerationSettings
    {

    }

    public static final SecretBaseDimension DIMENSION = new SecretBaseDimension();
    public static RegistryKey<World>             TYPE;
    public static Biome                     BIOME;

    public static final String ID = PokecubeCore.MODID + ":secret_bases";

    private static final ResourceLocation IDLOC = new ResourceLocation(SecretBaseDimension.ID);

    @SubscribeEvent
    public static void register(final RegisterDimensionsEvent event)
    {
        SecretBaseDimension.TYPE = DimensionManager.registerOrGetDimension(SecretBaseDimension.DIMENSION
                .getRegistryName(), SecretBaseDimension.DIMENSION, new PacketBuffer(Unpooled.EMPTY_BUFFER), true);
        DimensionManager.keepLoaded(SecretBaseDimension.TYPE);
    }

    @SubscribeEvent
    @OnlyIn(value = Dist.CLIENT)
    public static void clientTick(final ClientTickEvent event)
    {
        final World world = PokecubeCore.proxy.getWorld();
        if (world == null) return;
        if (world.getWorldBorder().getSize() != 2999984 && world.getDimensionKey().getRegistryName().equals(
                SecretBaseDimension.IDLOC)) world.getWorldBorder().setSize(2999984);
    }

    @SubscribeEvent
    public static void worldTick(final WorldTickEvent event)
    {
        final World world = event.world;
        if (world.getWorldBorder().getSize() != 2999984 && world.getDimensionKey().getRegistryName().equals(
                SecretBaseDimension.IDLOC)) world.getWorldBorder().setSize(2999984);
    }

    @SubscribeEvent
    public static void worldLoad(final WorldEvent.Load event)
    {
        final IWorld world = event.getWorld();
        if (world.getWorldBorder().getSize() != 2999984 && world.getDimensionKey().getRegistryName().equals(
                SecretBaseDimension.IDLOC)) world.getWorldBorder().setSize(2999984);
    }

    @Override
    public BiFunction<World, DimensionType, ? extends Dimension> getFactory()
    {
        return (w, t) -> new SecretDimension(w, t);
    }

    public static List<GlobalPos> getNearestBases(final GlobalPos here, final int baseRadarRange)
    {
        final List<GlobalPos> bases = Lists.newArrayList();
        for (final GlobalPos v : PokecubeSerializer.getInstance().bases)
            if (v.getPos().withinDistance(here.getPos(), baseRadarRange)) bases.add(v);
        return bases;
    }

}
