package com.example.lab5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.lab5.data.entity.ActiveSubstance;
import com.example.lab5.data.entity.ConflictRule;
import com.example.lab5.data.entity.Medicine;
import com.example.lab5.data.entity.MedicineWithDetails;
import com.example.lab5.domain.DetectedConflict;
import com.example.lab5.domain.InteractionChecker;
import com.example.lab5.domain.RiskSummary;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Покрыл ключевые сценарии проверки совместимости препаратов модульными тестами без запуска эмулятора.
public class Lab5UnitTest {

    private InteractionChecker checker;
    private ActiveSubstance ibuprofen;
    private ActiveSubstance aspirin;
    private ActiveSubstance warfarin;
    private ActiveSubstance paracetamol;
    private ActiveSubstance caffeine;
    private ActiveSubstance omeprazole;
    private List<ConflictRule> standardRules;

    @Before
    public void setUp() {
        checker = new InteractionChecker();
        ibuprofen = new ActiveSubstance(1, "Ибупрофен", "НПВП");
        aspirin = new ActiveSubstance(2, "Аспирин", "Антиагрегант");
        warfarin = new ActiveSubstance(3, "Варфарин", "Антикоагулянт");
        paracetamol = new ActiveSubstance(4, "Парацетамол", "Жаропонижающее");
        caffeine = new ActiveSubstance(5, "Кофеин", "Стимулятор");
        omeprazole = new ActiveSubstance(6, "Омепразол", "Защита желудка");

        standardRules = Arrays.asList(
                new ConflictRule(1, 1, 2, ConflictRule.SEVERITY_HIGH, "Риск кровотечения", "Обсудите замену"),
                new ConflictRule(2, 3, 1, ConflictRule.SEVERITY_HIGH, "Усиление кровоточивости", "Не совмещать"),
                new ConflictRule(3, 4, 5, ConflictRule.SEVERITY_MEDIUM, "Нагрузка на сердце", "Соблюдайте дозу"),
                new ConflictRule(4, 1, 6, ConflictRule.SEVERITY_LOW, "Замедление всасывания", "Разнесите приём")
        );
    }

    @Test
    public void noConflictsWhenSubstancesAreSafe() {
        List<MedicineWithDetails> kit = Arrays.asList(
                makeMedicine(10, "Панадол", paracetamol),
                makeMedicine(11, "Омез", omeprazole)
        );

        RiskSummary summary = checker.evaluate(kit, standardRules);

        assertFalse(summary.hasConflicts());
        assertEquals(0, summary.conflicts.size());
        assertEquals(0, summary.riskScore);
        assertEquals(RiskSummary.LEVEL_NONE, summary.overallLevel);
    }

    @Test
    public void detectsHighSeverityConflict() {
        List<MedicineWithDetails> kit = Arrays.asList(
                makeMedicine(10, "Нурофен", ibuprofen),
                makeMedicine(11, "Кардиомагнил", aspirin)
        );

        RiskSummary summary = checker.evaluate(kit, standardRules);

        assertTrue(summary.hasConflicts());
        assertEquals(1, summary.conflicts.size());
        assertEquals(1, summary.highCount);
        assertEquals(0, summary.mediumCount);
        assertEquals(0, summary.lowCount);
        assertEquals(RiskSummary.LEVEL_HIGH, summary.overallLevel);
        assertEquals(45, summary.riskScore);
    }

    @Test
    public void matchesRuleRegardlessOfSubstanceOrder() {
        // В правиле указано (1, 2), а в списке передаю сначала 2 (Аспирин), потом 1 (Ибупрофен).
        List<MedicineWithDetails> kitReversed = Arrays.asList(
                makeMedicine(20, "Кардиомагнил", aspirin),
                makeMedicine(21, "Нурофен", ibuprofen)
        );
        List<ConflictRule> reversedRuleOnly = Collections.singletonList(
                new ConflictRule(99, 2, 1, ConflictRule.SEVERITY_HIGH, "Обратное правило", "Проверка порядка")
        );

        RiskSummary summaryForward = checker.evaluate(kitReversed, standardRules);
        RiskSummary summaryReverse = checker.evaluate(kitReversed, reversedRuleOnly);

        assertEquals(1, summaryForward.conflicts.size());
        assertEquals(1, summaryReverse.conflicts.size());
        assertEquals(ConflictRule.SEVERITY_HIGH, summaryReverse.conflicts.get(0).severity);
    }

    @Test
    public void sortsMultipleConflictsBySeverityDescending() {
        List<MedicineWithDetails> kit = Arrays.asList(
                makeMedicine(1, "Омез", omeprazole),
                makeMedicine(2, "Нурофен", ibuprofen),
                makeMedicine(3, "Панадол", paracetamol),
                makeMedicine(4, "Цитрамон", caffeine),
                makeMedicine(5, "Кардиомагнил", aspirin)
        );

        RiskSummary summary = checker.evaluate(kit, standardRules);

        assertEquals(3, summary.conflicts.size());
        assertEquals(ConflictRule.SEVERITY_HIGH, summary.conflicts.get(0).severity);
        assertEquals(ConflictRule.SEVERITY_MEDIUM, summary.conflicts.get(1).severity);
        assertEquals(ConflictRule.SEVERITY_LOW, summary.conflicts.get(2).severity);
        assertEquals(1, summary.highCount);
        assertEquals(1, summary.mediumCount);
        assertEquals(1, summary.lowCount);
        assertEquals(77, summary.riskScore);
        assertEquals(RiskSummary.LEVEL_HIGH, summary.overallLevel);
    }

    @Test
    public void deduplicatesSamePairAndKeepsHigherSeverity() {
        List<MedicineWithDetails> kit = Arrays.asList(
                makeMedicine(1, "Нурофен", ibuprofen),
                makeMedicine(2, "Кардиомагнил", aspirin)
        );
        List<ConflictRule> duplicatedRules = Arrays.asList(
                new ConflictRule(1, 1, 2, ConflictRule.SEVERITY_LOW, "Слабое предупреждение", "Разнести"),
                new ConflictRule(2, 2, 1, ConflictRule.SEVERITY_HIGH, "Сильное предупреждение", "Отменить")
        );

        RiskSummary summary = checker.evaluate(kit, duplicatedRules);

        assertEquals(1, summary.conflicts.size());
        DetectedConflict conflict = summary.conflicts.get(0);
        assertEquals(ConflictRule.SEVERITY_HIGH, conflict.severity);
        assertEquals("Сильное предупреждение", conflict.rule.title);
    }

    @Test
    public void clampsRiskScoreAt100WithMultipleHighConflicts() {
        List<MedicineWithDetails> kit = Arrays.asList(
                makeMedicine(1, "Нурофен", ibuprofen),
                makeMedicine(2, "Кардиомагнил", aspirin),
                makeMedicine(3, "Варфарин Никомед", warfarin)
        );
        List<ConflictRule> heavyRules = Arrays.asList(
                new ConflictRule(1, 1, 2, ConflictRule.SEVERITY_HIGH, "Конфликт 1", "Совет 1"),
                new ConflictRule(2, 3, 1, ConflictRule.SEVERITY_HIGH, "Конфликт 2", "Совет 2"),
                new ConflictRule(3, 3, 2, ConflictRule.SEVERITY_HIGH, "Конфликт 3", "Совет 3")
        );

        RiskSummary summary = checker.evaluate(kit, heavyRules);

        assertEquals(3, summary.highCount);
        assertEquals(100, summary.riskScore);
        assertEquals(RiskSummary.LEVEL_HIGH, summary.overallLevel);
        assertEquals(
                ConflictRule.SEVERITY_HIGH,
                checker.getHighestSeverityForSubstance(warfarin.id, summary.conflicts)
        );
    }

    @Test
    public void handlesEmptyAndNullListsWithoutCrashing() {
        RiskSummary emptySummary = checker.evaluate(Collections.emptyList(), standardRules);
        RiskSummary nullSummary = checker.evaluate(null, null);

        assertEquals(RiskSummary.LEVEL_NONE, emptySummary.overallLevel);
        assertEquals(0, emptySummary.riskScore);
        assertEquals(RiskSummary.LEVEL_NONE, nullSummary.overallLevel);
        assertEquals(0, nullSummary.conflicts.size());
    }

    private MedicineWithDetails makeMedicine(int id, String name, ActiveSubstance substance) {
        MedicineWithDetails details = new MedicineWithDetails();
        details.medicine = new Medicine(id, name, "500 мг", substance.id, "09:00", "");
        details.substance = substance;
        details.intakes = Collections.emptyList();
        return details;
    }
}
