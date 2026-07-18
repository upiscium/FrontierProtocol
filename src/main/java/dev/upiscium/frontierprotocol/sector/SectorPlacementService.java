package dev.upiscium.frontierprotocol.sector;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import net.minecraft.resources.ResourceLocation;

public final class SectorPlacementService {
    private static final Comparator<SectorTraitDefinition> TRAIT_ORDER = Comparator
            .comparingInt(SectorTraitDefinition::priority).reversed()
            .thenComparing(definition -> definition.id().toString());
    private final Supplier<List<SectorTraitDefinition>> definitions;
    private final LongSupplier revision;
    private final Map<CacheKey, Optional<ResourceLocation>> cache = new LinkedHashMap<>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Optional<ResourceLocation>> eldest) {
            return size() > 4096;
        }
    };

    public SectorPlacementService(Supplier<List<SectorTraitDefinition>> definitions) {
        this(definitions, () -> 0L);
    }

    public SectorPlacementService(Supplier<List<SectorTraitDefinition>> definitions, LongSupplier revision) {
        this.definitions = definitions;
        this.revision = revision;
    }

    public Optional<ResourceLocation> resolve(long worldSeed, SectorPos sector, Map<SectorPos, ResourceLocation> overrides) {
        return resolve(worldSeed, sector, new SectorPos(0, 0), overrides);
    }

    public Optional<ResourceLocation> resolve(long worldSeed, SectorPos sector, SectorPos origin,
            Map<SectorPos, ResourceLocation> overrides) {
        ResourceLocation override = overrides.get(sector);
        if (override != null) return Optional.of(override);
        CacheKey key = new CacheKey(worldSeed, sector, origin, revision.getAsLong());
        synchronized (cache) {
            Optional<ResourceLocation> cached = cache.get(key);
            if (cached != null) return cached;
        }
        Optional<ResourceLocation> resolved = orderedDefinitions().stream()
                    .filter(definition -> naturallyContains(worldSeed, definition, sector, origin))
                    .map(SectorTraitDefinition::id)
                    .findFirst();
        synchronized (cache) {
            cache.put(key, resolved);
        }
        return resolved;
    }

    public boolean naturallyContains(long worldSeed, SectorTraitDefinition definition, SectorPos query) {
        return naturallyContains(worldSeed, definition, query, new SectorPos(0, 0));
    }

    public boolean naturallyContains(long worldSeed, SectorTraitDefinition definition, SectorPos query, SectorPos origin) {
        SectorTraitDefinition.Placement placement = definition.placement();
        long radius = placement.clusterMax() - 1L;
        long usable = placement.spacing() - placement.separation();
        long minCellX = ceilDiv((long) query.x() - radius - (usable - 1), placement.spacing());
        long maxCellX = Math.floorDiv((long) query.x() + radius, placement.spacing());
        long minCellZ = ceilDiv((long) query.z() - radius - (usable - 1), placement.spacing());
        long maxCellZ = Math.floorDiv((long) query.z() + radius, placement.spacing());
        for (long cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (long cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                Set<SectorPos> footprint = footprint(worldSeed, definition, cellX, cellZ, origin);
                if (footprint.contains(query)) return true;
            }
        }
        return false;
    }

    public List<SectorTraitDefinition> orderedDefinitions() {
        return definitions.get().stream().sorted(TRAIT_ORDER).toList();
    }

    private static Set<SectorPos> footprint(long seed, SectorTraitDefinition definition, long cellX, long cellZ, SectorPos origin) {
        SectorTraitDefinition.Placement placement = definition.placement();
        long idHash = idHash(definition.id());
        int usable = placement.spacing() - placement.separation();
        long anchorX = cellX * placement.spacing() + bounded(hash(seed, idHash, placement.salt(), cellX, cellZ, 0, 0), usable);
        long anchorZ = cellZ * placement.spacing() + bounded(hash(seed, idHash, placement.salt(), cellX, cellZ, 1, 0), usable);
        if (anchorX < Integer.MIN_VALUE || anchorX > Integer.MAX_VALUE || anchorZ < Integer.MIN_VALUE || anchorZ > Integer.MAX_VALUE) return Set.of();
        if (!passesChance(hash(seed, idHash, placement.salt(), cellX, cellZ, 2, 0), placement.anchorChance())) return Set.of();

        SectorPos anchor = new SectorPos((int) anchorX, (int) anchorZ);
        int sizeRange = placement.clusterMax() - placement.clusterMin() + 1;
        int targetSize = placement.clusterMin() + bounded(hash(seed, idHash, placement.salt(), cellX, cellZ, 3, 0), sizeRange);
        Set<SectorPos> occupied = new HashSet<>();
        TreeSet<SectorPos> frontier = new TreeSet<>();
        occupied.add(anchor);
        addNeighbors(anchor, occupied, frontier);
        for (int index = 1; index < targetSize && !frontier.isEmpty(); index++) {
            int selectedIndex = bounded(hash(seed, idHash, placement.salt(), cellX, cellZ, 4, index), frontier.size());
            SectorPos selected = new ArrayList<>(frontier).get(selectedIndex);
            frontier.remove(selected);
            occupied.add(selected);
            addNeighbors(selected, occupied, frontier);
        }
        for (SectorPos member : occupied) {
            long distance = member.chebyshevDistance(origin);
            if (distance < placement.minDistance() || placement.maxDistance().filter(max -> distance > max).isPresent()) return Set.of();
        }
        return occupied;
    }

    private static void addNeighbors(SectorPos center, Set<SectorPos> occupied, Set<SectorPos> frontier) {
        addNeighbor(center, 1, 0, occupied, frontier);
        addNeighbor(center, -1, 0, occupied, frontier);
        addNeighbor(center, 0, 1, occupied, frontier);
        addNeighbor(center, 0, -1, occupied, frontier);
    }

    private static void addNeighbor(SectorPos center, int dx, int dz, Set<SectorPos> occupied, Set<SectorPos> frontier) {
        long x = (long) center.x() + dx;
        long z = (long) center.z() + dz;
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE) return;
        SectorPos neighbor = new SectorPos((int) x, (int) z);
        if (!occupied.contains(neighbor)) frontier.add(neighbor);
    }

    public static long stableOrderHash(long seed, ResourceLocation id, long salt, SectorPos pos) {
        return hash(seed, idHash(id), salt, pos.x(), pos.z(), 5, 0);
    }

    private static long ceilDiv(long value, long divisor) {
        return -Math.floorDiv(-value, divisor);
    }

    private static boolean passesChance(long value, double chance) {
        if (chance <= 0) return false;
        if (chance >= 1) return true;
        return (value >>> 11) * 0x1.0p-53 < chance;
    }

    private static int bounded(long value, int bound) {
        return (int) Long.remainderUnsigned(value, Integer.toUnsignedLong(bound));
    }

    private static long idHash(ResourceLocation id) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : id.toString().getBytes(StandardCharsets.UTF_8)) {
            hash ^= Byte.toUnsignedInt(value);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long hash(long seed, long idHash, long salt, long x, long z, long stream, long index) {
        long hash = mix64(seed ^ 0x243f6a8885a308d3L);
        hash = mix64(hash ^ idHash);
        hash = mix64(hash ^ salt);
        hash = mix64(hash ^ x * 0x9e3779b97f4a7c15L);
        hash = mix64(hash ^ z * 0xc2b2ae3d27d4eb4fL);
        hash = mix64(hash ^ stream * 0x165667b19e3779f9L);
        return mix64(hash ^ index * 0xd6e8feb86659fd93L);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private record CacheKey(long seed, SectorPos sector, SectorPos origin, long revision) {}
}
