package dev.manestack.service;

import dev.manestack.jooq.generated.tables.records.PokerTableRecord;
import dev.manestack.service.poker.core.GameTable;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dev.manestack.jooq.generated.Tables.*;

@ApplicationScoped
public class GameService {
    private static final Logger LOG = Logger.getLogger(UserService.class);
    private final ExecutorService QUERY_THREADS = Executors.newFixedThreadPool(3);
    public final Map<Long, GameTable> TABLES = new HashMap<>();

    @Inject
    DSLContext context;

    public void init(@Observes StartupEvent ignored) {
        fetchTables().invoke(tables -> {
                    for (GameTable table : tables) {
                        TABLES.put(table.getTableId(), table);
                    }
                })
                .subscribe().with(unused -> {
                });
    }

    public Uni<List<GameTable>> fetchTables() {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> context.selectFrom(POKER_TABLE)
                        .fetchInto(GameTable.class));
    }

    public Uni<GameTable> createTable(Integer userId, GameTable table) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    table.validateCreate();
                    PokerTableRecord pokerTableRecord = context.insertInto(POKER_TABLE)
                            .set(POKER_TABLE.TABLE_NAME, table.getTableName())
                            .set(POKER_TABLE.MAX_PLAYERS, table.getMaxPlayers())
                            .set(POKER_TABLE.BIG_BLIND, table.getBigBlind())
                            .set(POKER_TABLE.SMALL_BLIND, table.getSmallBlind())
                            .set(POKER_TABLE.MIN_BUY_IN, table.getMinBuyIn())
                            .set(POKER_TABLE.MAX_BUY_IN, table.getMaxBuyIn())
                            .set(POKER_TABLE.VARIANT, table.getVariant())
                            .set(POKER_TABLE.CREATED_AT, OffsetDateTime.now())
                            .set(POKER_TABLE.CREATED_BY, userId)
                            .returning(POKER_TABLE.TABLE_ID)
                            .fetchOne();
                    if (pokerTableRecord != null) {
                        table.setTableId(pokerTableRecord.getTableId());
                        table.setCreatedAt(pokerTableRecord.getCreatedAt());
                        table.setCreatedBy(pokerTableRecord.getCreatedBy());
                        TABLES.put(table.getTableId(), table);
                        LOG.infov("Created table {0}", table.getTableName());
                        return table;
                    } else {
                        LOG.errorv("Failed to create table {0}", table.getTableName());
                        throw new RuntimeException("Failed to create table");
                    }
                });
    }

    public Uni<GameTable> updateTable(GameTable table) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    UpdateSetFirstStep<?> update = context.update(POKER_TABLE);
                    UpdateSetMoreStep<?> updateSetMoreStep = null;
                    if (table.getTableName() != null)
                        updateSetMoreStep = update.set(POKER_TABLE.TABLE_NAME, table.getTableName());
                    if (table.getMaxPlayers() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.MAX_PLAYERS, table.getMaxPlayers()) : update.set(POKER_TABLE.MAX_PLAYERS, table.getMaxPlayers());
                    if (table.getBigBlind() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.BIG_BLIND, table.getBigBlind()) : update.set(POKER_TABLE.BIG_BLIND, table.getBigBlind());
                    if (table.getSmallBlind() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.SMALL_BLIND, table.getSmallBlind()) : update.set(POKER_TABLE.SMALL_BLIND, table.getSmallBlind());
                    if (table.getMinBuyIn() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.MIN_BUY_IN, table.getMinBuyIn()) : update.set(POKER_TABLE.MIN_BUY_IN, table.getMinBuyIn());
                    if (table.getMaxBuyIn() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.MAX_BUY_IN, table.getMaxBuyIn()) : update.set(POKER_TABLE.MAX_BUY_IN, table.getMaxBuyIn());
                    if (table.getVariant() != null)
                        updateSetMoreStep = updateSetMoreStep != null ? updateSetMoreStep.set(POKER_TABLE.VARIANT, table.getVariant()) : update.set(POKER_TABLE.VARIANT, table.getVariant());
                    if (updateSetMoreStep == null) {
                        LOG.errorv("No fields to update for table {0}", table.getTableName());
                        throw new RuntimeException("No fields to update");
                    }
                    GameTable updatedTable = updateSetMoreStep
                            .where(POKER_TABLE.TABLE_ID.eq(table.getTableId()))
                            .returning()
                            .fetchOneInto(GameTable.class);

                    if (updatedTable != null) {
                        LOG.infov("Updated table {0}", table.getTableName());
                        TABLES.put(table.getTableId(), updatedTable);
                        return updatedTable;
                    } else {
                        LOG.errorv("Failed to update table {0}", table.getTableName());
                        throw new RuntimeException("Failed to update table");
                    }
                });
    }

    public Uni<Void> deleteTable(Long tableId, Integer userId) {
        return Uni.createFrom().voidItem()
                .emitOn(QUERY_THREADS)
                .map(unused -> {
                    context.deleteFrom(POKER_TABLE)
                            .where(POKER_TABLE.TABLE_ID.eq(tableId))
                            .execute();
                    TABLES.remove(tableId);
                    LOG.infov("Deleted table {0}", tableId);
                    return null;
                });
    }
}
