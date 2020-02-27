package pokecube.core.world.gen.feature.scattered.jigsaw;

import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.MarginedStructureStart;
import net.minecraft.world.gen.feature.structure.ScatteredStructure;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.PokecubeCore;
import pokecube.core.database.worldgen.WorldgenHandler.JigSawConfig;
import pokecube.core.events.StructureEvent.PickLocation;
import pokecube.core.utils.PokecubeSerializer;

public class JigsawStructure extends ScatteredStructure<JigsawConfig>
{

    public final JigSawConfig struct;

    public JigsawStructure(final JigSawConfig struct)
    {
        super(JigsawConfig::deserialize);
        this.struct = struct;
    }

    @Override
    public String getStructureName()
    {
        return this.struct.name;
    }

    @Override
    public int getSize()
    {
        return 4;
    }

    @Override
    public boolean func_225558_a_(final BiomeManager biomeManager, final ChunkGenerator<?> chunkGen, final Random rand,
            final int chunkPosX, final int chunkPosZ, final Biome biome)
    {
        if (this.struct.atSpawn && (PokecubeSerializer.getInstance().customData.contains("start_pokecentre")
                || !PokecubeCore.getConfig().doSpawnBuilding))
            return false;
        final ChunkPos chunkpos = this.getStartPositionForPosition(chunkGen, rand, chunkPosX, chunkPosZ, 0, 0);

        if (chunkPosX == chunkpos.x && chunkPosZ == chunkpos.z)
        {
            final int i = chunkPosX >> 4;
            final int j = chunkPosZ >> 4;
            rand.setSeed(i ^ j << 4 ^ chunkGen.getSeed());
            rand.nextFloat();
            if (rand.nextFloat() > this.struct.chance) return false;
            if (chunkGen.hasStructure(biome, this))
            {
                final boolean valid = !MinecraftForge.EVENT_BUS.post(new PickLocation(chunkGen, rand, chunkPosX,
                        chunkPosZ, this.struct));
                if (valid && this.struct.atSpawn)
                {
                    PokecubeSerializer.getInstance().customData.putBoolean("start_pokecentre", true);
                    PokecubeSerializer.getInstance().save();
                }
                return valid;
            }
        }
        return false;
    }

    @Override
    protected int getBiomeFeatureDistance(final ChunkGenerator<?> chunkGenerator)
    {
        return this.struct.distance;
    }

    @Override
    protected int getBiomeFeatureSeparation(final ChunkGenerator<?> chunkGenerator)
    {
        return this.struct.separation;
    }

    @Override
    public Structure.IStartFactory getStartFactory()
    {
        return JigsawStructure.Start::new;
    }

    @Override
    protected int getSeedModifier()
    {
        return 165746796;
    }

    public static class Start extends MarginedStructureStart
    {
        public Start(final Structure<?> struct, final int x, final int z, final MutableBoundingBox box, final int ref,
                final long seed)
        {
            super(struct, x, z, box, ref, seed);
        }

        @Override
        public void init(final ChunkGenerator<?> generator, final TemplateManager templateManagerIn, final int chunkX,
                final int chunkZ, final Biome biomeIn)
        {
            if (this.getStructure() instanceof JigsawStructure)
            {
                final BlockPos blockpos = new BlockPos(chunkX * 16, 90, chunkZ * 16);
                JigsawPieces.initStructure(generator, templateManagerIn, blockpos, this.components, this.rand,
                        ((JigsawStructure) this.getStructure()).struct);
                PokecubeCore.LOGGER.debug("Placing structure {} at {} {} {} composed of {} parts ",
                        ((JigsawStructure) this.getStructure()).struct.name, blockpos.getX(), blockpos.getY(),
                        blockpos.getZ(), this.components.size());
                this.recalculateStructureSize();
            }
        }
    }
}