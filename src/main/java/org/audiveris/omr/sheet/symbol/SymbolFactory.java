//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l F a c t o r y                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.sheet.ProcessingSwitches;
import org.audiveris.omr.sheet.ProcessingSwitches.Switch;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.TimeBuilder;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BreathMarkInter;
import org.audiveris.omr.sig.inter.CaesuraInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code SymbolFactory} generates the inter instances corresponding to
 * to an acceptable symbol evaluation in a given system.
 * <p>
 * (Generally there is one inter instance per evaluation, an exception is the case of full time
 * signature which leads to upper plus lower number instances).
 *
 * @author Hervé Bitteur
 */
public class SymbolFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** All system stems, ordered by abscissa. */
    private final List<Inter> systemStems;

    /** All system heads, ordered by abscissa. */
    private final List<Inter> systemHeads;

    /** All system notes (heads and rests), ordered by abscissa. */
    private List<Inter> systemNotes;

    /** All system head-based chords, ordered by abscissa. */
    private final List<Inter> systemHeadChords;

    /** All system rests, ordered by abscissa. */
    private List<Inter> systemRests;

    /** All system bar lines, ordered by abscissa. */
    private List<Inter> systemBars;

    /** Dot factory companion. */
    private final DotFactory dotFactory;

    /** Processing switches. */
    private final ProcessingSwitches switches;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsFactory object.
     *
     * @param system the dedicated system
     */
    public SymbolFactory (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();

        systemStems = sig.inters(Shape.STEM);
        Collections.sort(systemStems, Inters.byAbscissa);

        systemHeads = sig.inters(HeadInter.class);
        Collections.sort(systemHeads, Inters.byAbscissa);

        systemHeadChords = sig.inters(AbstractChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        dotFactory = new DotFactory(this, system);

        switches = system.getSheet().getStub().getProcessingSwitches();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * Create and add to SIG the proper inter instance(s) for provided evaluated glyph.
     *
     * @param eval         evaluation result
     * @param glyph        evaluated glyph
     * @param closestStaff only the closest staff, ordinate-wise
     */
    public void create (Evaluation eval,
                        Glyph glyph,
                        Staff closestStaff)
    {
        final Inter inter = doCreate(eval, glyph, closestStaff);

        // Add to SIG if not already done
        if ((inter != null) && (inter.getSig() == null)) {
            sig.addVertex(inter);
        }
    }

    //--------------//
    // createManual //
    //--------------//
    /**
     * Create a manual inter instance to handle the provided shape.
     *
     * @param shape provided shape
     * @param sheet related sheet
     * @return the created manual inter or null
     */
    public static Inter createManual (Shape shape,
                                      Sheet sheet)
    {
        Inter ghost = doCreateManual(shape, sheet);

        if (ghost != null) {
            ghost.setManual(true);
        }

        return ghost;
    }

    //---------------//
    // getSystemBars //
    //---------------//
    public List<Inter> getSystemBars ()
    {
        if (systemBars == null) {
            systemBars = sig.inters(BarlineInter.class);
            Collections.sort(systemBars, Inters.byAbscissa);
        }

        return systemBars;
    }

    //----------------//
    // getSystemRests //
    //----------------//
    public List<Inter> getSystemRests ()
    {
        if (systemRests == null) {
            systemRests = sig.inters(RestInter.class);
            Collections.sort(systemRests, Inters.byAbscissa);
        }

        return systemRests;
    }

    //------------//
    // lateChecks //
    //------------//
    public void lateChecks ()
    {
        // Conflicting dot interpretations
        dotFactory.lateDotChecks();

        // Complex dynamics
        handleComplexDynamics();

        // Column consistency of Time Signatures in a system
        handleTimes();
    }

    //---------------------//
    // getSystemHeadChords //
    //---------------------//
    List<Inter> getSystemHeadChords ()
    {
        return systemHeadChords;
    }

    //----------------//
    // getSystemHeads //
    //----------------//
    List<Inter> getSystemHeads ()
    {
        return systemHeads;
    }

    //----------------//
    // getSystemNotes //
    //----------------//
    List<Inter> getSystemNotes ()
    {
        if (systemNotes == null) {
            systemNotes = new ArrayList<Inter>(getSystemHeads().size() + getSystemRests().size());
            systemNotes.addAll(getSystemHeads());
            systemNotes.addAll(getSystemRests());
            Collections.sort(systemNotes, Inters.byAbscissa);
        }

        return systemNotes;
    }

    //----------//
    // doCreate //
    //----------//
    /**
     * Create proper inter instance(s) for provided evaluated glyph.
     * <p>
     * This method addresses only the symbols handled via a classifier.
     * In current OMR design, this does not address: <ul>
     * <li>Brace
     * <li>Bracket
     * <li>Barline
     * <li>Beam, Beam_hook
     * <li>Head
     * <li>Fermata, Fermata_below (not yet directly handled by OMR engine)
     * <li>Ledger
     * <li>Stem
     * <li>Curve
     * <li>Text, Lyrics
     * </ul>
     *
     * @param eval         evaluation result
     * @param glyph        evaluated glyph
     * @param closestStaff only the closest staff, ordinate-wise
     */
    private Inter doCreate (Evaluation eval,
                            Glyph glyph,
                            Staff closestStaff)
    {
        final Shape shape = eval.shape;
        final double grade = Grades.intrinsicRatio * eval.grade;

        if (glyph.isVip()) {
            logger.info("VIP glyph#{} symbol created as {}", glyph.getId(), eval.shape);
        }

        switch (shape) {
        case CLUTTER:
            return null;

        // Ottava (TODO: not yet handled ???)
        //
        case OTTAVA_ALTA:
        case OTTAVA_BASSA:
            return null;

        // All dots:
        // - REPEAT_DOT
        // - FERMATA_DOT
        // - STACCATO_DOT
        // - AUGMENTATION_DOT
        case DOT_set:
            dotFactory.instantDotChecks(eval, glyph);

            return null;

        // Clefs
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
        case C_CLEF:
        case PERCUSSION_CLEF:
            return ClefInter.create(glyph, shape, grade, closestStaff); // Staff is OK

        // Key signatures
        case KEY_FLAT_1:
        case KEY_FLAT_2:
        case KEY_FLAT_3:
        case KEY_FLAT_4:
        case KEY_FLAT_5:
        case KEY_FLAT_6:
        case KEY_FLAT_7:
        case KEY_SHARP_1:
        case KEY_SHARP_2:
        case KEY_SHARP_3:
        case KEY_SHARP_4:
        case KEY_SHARP_5:
        case KEY_SHARP_6:
        case KEY_SHARP_7:
            return new KeyInter(grade, shape);

        // Time sig
        //        case TIME_ZERO:
        //        case TIME_ONE:
        case TIME_TWO:
        case TIME_THREE:
        case TIME_FOUR:
        case TIME_FIVE:
        case TIME_SIX:
        case TIME_SEVEN:
        case TIME_EIGHT:
        case TIME_NINE:
        case TIME_TWELVE:
        case TIME_SIXTEEN:
            return TimeNumberInter.create(glyph, shape, grade, closestStaff); // Staff is OK

        case COMMON_TIME:
        case CUT_TIME:
        case TIME_FOUR_FOUR:
        case TIME_TWO_TWO:
        case TIME_TWO_FOUR:
        case TIME_THREE_FOUR:
        case TIME_FIVE_FOUR:
        case TIME_THREE_EIGHT:
        case TIME_SIX_EIGHT:
            return TimeWholeInter.create(glyph, shape, grade, closestStaff); // Staff is OK

        // Flags
        case FLAG_1:
        case FLAG_2:
        case FLAG_3:
        case FLAG_4:
        case FLAG_5:
        case FLAG_1_UP:
        case FLAG_2_UP:
        case FLAG_3_UP:
        case FLAG_4_UP:
        case FLAG_5_UP:
            return AbstractFlagInter.createValidAdded(glyph, shape, grade, system, systemStems); // Glyph is checked

        case SMALL_FLAG:
        case SMALL_FLAG_SLASH:
            return new SmallFlagInter(null, shape, grade);

        // Rests
        case LONG_REST:
        case BREVE_REST:
        case WHOLE_REST:
        case HALF_REST:
        case QUARTER_REST:
        case EIGHTH_REST:
        case ONE_16TH_REST:
        case ONE_32ND_REST:
        case ONE_64TH_REST:
        case ONE_128TH_REST:
            return RestInter.createValid(glyph, shape, grade, system, systemHeadChords);

        // Tuplets
        case TUPLET_THREE:
        case TUPLET_SIX:
            return TupletInter.createValid(glyph, shape, grade, system, systemHeadChords);

        // Accidentals
        case FLAT:
        case NATURAL:
        case SHARP:
        case DOUBLE_SHARP:
        case DOUBLE_FLAT: {
            AlterInter alter = AlterInter.create(glyph, shape, grade, closestStaff); // Staff is very questionable!

            sig.addVertex(alter);
            alter.setLinks(systemHeads);

            return alter;
        }

        // Articulations
        case ACCENT:
        case TENUTO:
        case STACCATO:
        case STACCATISSIMO:
        case STRONG_ACCENT:
            return switches.getValue(Switch.articulations)
                    ? ArticulationInter.createValidAdded(
                            glyph,
                            shape,
                            grade,
                            system,
                            systemHeadChords) : null;

        // Markers
        case CODA:
        case SEGNO:
        case DAL_SEGNO:
        case DA_CAPO: {
            MarkerInter marker = MarkerInter.create(glyph, shape, grade, closestStaff); // OK

            sig.addVertex(marker);
            marker.linkWithStaffBarline();

            return marker;
        }

        // Holds
        case FERMATA_ARC:
        case FERMATA_ARC_BELOW:
            return FermataArcInter.create(glyph, shape, grade, system);

        case CAESURA:
            return CaesuraInter.create(glyph, grade, system);

        case BREATH_MARK:
            return BreathMarkInter.create(glyph, grade, system);

        // Dynamics
        case DYNAMICS_P:
        case DYNAMICS_PP:
        case DYNAMICS_MP:
        case DYNAMICS_F:
        case DYNAMICS_FF:
        case DYNAMICS_MF:
        case DYNAMICS_FP:
        case DYNAMICS_SF:
        case DYNAMICS_SFZ:
            return new DynamicsInter(glyph, shape, grade);

        // Wedges
        case CRESCENDO:
        case DIMINUENDO:
            return new WedgeInter(glyph, shape, grade);

        // Ornaments (TODO: Really handle GRACE_NOTE and GRACE_NOTE_SLASH)
        case GRACE_NOTE_SLASH:
        case GRACE_NOTE:
        case TR:
        case TURN:
        case TURN_INVERTED:
        case TURN_UP:
        case TURN_SLASH:
        case MORDENT:
        case MORDENT_INVERTED:
            return OrnamentInter.createValidAdded(glyph, shape, grade, system, systemHeadChords);

        // Plucked techniques
        case ARPEGGIATO:
            return ArpeggiatoInter.createValidAdded(glyph, grade, system, systemHeadChords);

        // Keyboards
        case PEDAL_MARK:
        case PEDAL_UP_MARK:
            return new PedalInter(glyph, shape, grade);

        // Fingering
        case DIGIT_0:
        case DIGIT_1:
        case DIGIT_2:
        case DIGIT_3:
        case DIGIT_4:
        case DIGIT_5:
            return switches.getValue(Switch.fingerings) ? new FingeringInter(glyph, shape, grade)
                    : null;

        // Plucking
        case PLUCK_P:
        case PLUCK_I:
        case PLUCK_M:
        case PLUCK_A:
            return switches.getValue(Switch.pluckings) ? new PluckingInter(glyph, shape, grade) : null;

        // Romans
        case ROMAN_I:
        case ROMAN_II:
        case ROMAN_III:
        case ROMAN_IV:
        case ROMAN_V:
        case ROMAN_VI:
        case ROMAN_VII:
        case ROMAN_VIII:
        case ROMAN_IX:
        case ROMAN_X:
        case ROMAN_XI:
        case ROMAN_XII:
            return switches.getValue(Switch.frets) ? new FretInter(glyph, shape, grade) : null;

        // Others
        default:
            logger.info("No support yet for {}", shape);

            return null;
        }
    }

    //----------------//
    // doCreateManual //
    //----------------//
    /**
     * Create a not-yet-settled inter instance to handle the provided shape.
     * <p>
     * This method is meant to address all shapes for which a manual inter can be created.
     *
     * @param shape provided shape
     * @param sheet related sheet
     * @return the created ghost inter or null
     */
    private static Inter doCreateManual (Shape shape,
                                         Sheet sheet)
    {
        final ProcessingSwitches switches = sheet.getStub().getProcessingSwitches();
        final double GRADE = 1.0; // Grade value for any manual shape

        switch (shape) {
        //
        // Ottava TODO ???
        case OTTAVA_ALTA:
        case OTTAVA_BASSA:
            return null;

        // Brace, bracket TODO ???
        //
        // Barlines
        case THIN_BARLINE:
        case THICK_BARLINE:

            if (sheet.getStub().getLatestStep().compareTo(Step.MEASURES) < 0) {
                return new BarlineInter(null, shape, GRADE, null, null);
            } else {
                return new StaffBarlineInter(shape, GRADE);
            }

        case DOUBLE_BARLINE:
        case FINAL_BARLINE:
        case REVERSE_FINAL_BARLINE:
        case LEFT_REPEAT_SIGN:
        case RIGHT_REPEAT_SIGN:
        case BACK_TO_BACK_REPEAT_SIGN:
            return new StaffBarlineInter(shape, GRADE);

        //
        // Beams
        case BEAM:
            return new BeamInter(GRADE);

        case BEAM_HOOK:
            return new BeamHookInter(GRADE);

        // Ledger
        case LEDGER:
            return new LedgerInter(null, GRADE);

        // Stem
        case STEM:
            return new StemInter(null, GRADE);

        // Repeats
        case REPEAT_DOT:
            return new RepeatDotInter(null, GRADE, null, null); // No visit

        // Curves
        case SLUR:
            return new SlurInter(GRADE);

        // Text
        case LYRICS:
            return new SentenceInter(GRADE);

        case TEXT:
            return new WordInter(GRADE);

        // Clefs
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
        case C_CLEF:
        case PERCUSSION_CLEF:
            return new ClefInter(shape, GRADE);

        // Key signatures
        case KEY_FLAT_1:
        case KEY_FLAT_2:
        case KEY_FLAT_3:
        case KEY_FLAT_4:
        case KEY_FLAT_5:
        case KEY_FLAT_6:
        case KEY_FLAT_7:
        case KEY_SHARP_1:
        case KEY_SHARP_2:
        case KEY_SHARP_3:
        case KEY_SHARP_4:
        case KEY_SHARP_5:
        case KEY_SHARP_6:
        case KEY_SHARP_7:
            return new KeyInter(GRADE, shape);

        // Time sig
        case TIME_ZERO:
        case TIME_ONE:
        case TIME_TWO:
        case TIME_THREE:
        case TIME_FOUR:
        case TIME_FIVE:
        case TIME_SIX:
        case TIME_SEVEN:
        case TIME_EIGHT:
        case TIME_NINE:
        case TIME_TWELVE:
        case TIME_SIXTEEN:
            return new TimeNumberInter(null, shape, GRADE, null); // No visit

        case COMMON_TIME:
        case CUT_TIME:
        case TIME_FOUR_FOUR:
        case TIME_TWO_TWO:
        case TIME_TWO_FOUR:
        case TIME_THREE_FOUR:
        case TIME_FIVE_FOUR:
        case TIME_THREE_EIGHT:
        case TIME_SIX_EIGHT:
            return new TimeWholeInter(null, shape, GRADE);

        // Noteheads
        case NOTEHEAD_BLACK:
        case NOTEHEAD_BLACK_SMALL:
        case NOTEHEAD_VOID:
        case NOTEHEAD_VOID_SMALL:
        case BREVE:
        case WHOLE_NOTE:
        case WHOLE_NOTE_SMALL:
            return new HeadInter(null, null, null, shape, GRADE, null, null);

        case AUGMENTATION_DOT:
            return new AugmentationDotInter(null, GRADE); // No visit

        // Flags
        case FLAG_1:
        case FLAG_2:
        case FLAG_3:
        case FLAG_4:
        case FLAG_5:
        case FLAG_1_UP:
        case FLAG_2_UP:
        case FLAG_3_UP:
        case FLAG_4_UP:
        case FLAG_5_UP:
            return new FlagInter(null, shape, GRADE);

        case SMALL_FLAG:
        case SMALL_FLAG_SLASH:
            return new SmallFlagInter(null, shape, GRADE);

        // Rests
        case LONG_REST:
        case BREVE_REST:
        case WHOLE_REST:
        case HALF_REST:
        case QUARTER_REST:
        case EIGHTH_REST:
        case ONE_16TH_REST:
        case ONE_32ND_REST:
        case ONE_64TH_REST:
        case ONE_128TH_REST:
            return new RestInter(null, shape, GRADE, null, null); // No visit

        // Tuplets
        case TUPLET_THREE:
        case TUPLET_SIX:
            return new TupletInter(null, shape, GRADE); // No visit

        // Accidentals
        case FLAT:
        case NATURAL:
        case SHARP:
        case DOUBLE_SHARP:
        case DOUBLE_FLAT:
            return new AlterInter(null, shape, GRADE, null, null, null); // No visit

        // Articulations
        case ACCENT:
        case TENUTO:
        case STACCATO:
        case STACCATISSIMO:
        case STRONG_ACCENT:
            return switches.getValue(Switch.articulations)
                    ? new ArticulationInter(null, shape, GRADE) : null; // No visit

        // Markers
        case CODA:
        case SEGNO:
        case DAL_SEGNO:
        case DA_CAPO:
            return new MarkerInter(null, shape, GRADE); // No visit

        // Holds
        case FERMATA:
        case FERMATA_BELOW:
            return new FermataInter(shape, GRADE); // No visit

        case FERMATA_DOT:
            return new FermataDotInter(null, GRADE); // No visit

        case CAESURA:
            return new CaesuraInter(null, GRADE); // No visit

        case BREATH_MARK:
            return new BreathMarkInter(null, GRADE); // No visit

        // Dynamics
        case DYNAMICS_P:
        case DYNAMICS_PP:
        case DYNAMICS_MP:
        case DYNAMICS_F:
        case DYNAMICS_FF:
        case DYNAMICS_MF:
        case DYNAMICS_FP:
        case DYNAMICS_SF:
        case DYNAMICS_SFZ:
            return new DynamicsInter(null, shape, GRADE); // No visit

        // Wedges
        case CRESCENDO:
        case DIMINUENDO:
            return new WedgeInter(null, shape, GRADE); // ?

        // Ornaments
        case GRACE_NOTE_SLASH:
        case GRACE_NOTE:
        case TR:
        case TURN:
        case TURN_INVERTED:
        case TURN_UP:
        case TURN_SLASH:
        case MORDENT:
        case MORDENT_INVERTED:
            return new OrnamentInter(null, shape, GRADE); // No visit

        // Plucked techniques
        case ARPEGGIATO:
            return new ArpeggiatoInter(null, GRADE);

        // Keyboards
        case PEDAL_MARK:
        case PEDAL_UP_MARK:
            return new PedalInter(null, shape, GRADE); // No visit

        // Fingering
        case DIGIT_0:
        case DIGIT_1:
        case DIGIT_2:
        case DIGIT_3:
        case DIGIT_4:
        case DIGIT_5:
            return switches.getValue(Switch.fingerings) ? new FingeringInter(null, shape, GRADE)
                    : null; // No visit

        // Plucking
        case PLUCK_P:
        case PLUCK_I:
        case PLUCK_M:
        case PLUCK_A:
            return switches.getValue(Switch.pluckings) ? new PluckingInter(null, shape, GRADE) : null; // No visit

        // Romans
        case ROMAN_I:
        case ROMAN_II:
        case ROMAN_III:
        case ROMAN_IV:
        case ROMAN_V:
        case ROMAN_VI:
        case ROMAN_VII:
        case ROMAN_VIII:
        case ROMAN_IX:
        case ROMAN_X:
        case ROMAN_XI:
        case ROMAN_XII:
            return switches.getValue(Switch.frets) ? new FretInter(null, shape, GRADE) : null; // No visit

        // Others
        default:

            String msg = "No ghost instance for " + shape;
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    //-----------------------//
    // handleComplexDynamics //
    //-----------------------//
    /**
     * Handle competition between complex and shorter dynamics.
     */
    private void handleComplexDynamics ()
    {
        // All dynamics in system
        final List<Inter> dynamics = sig.inters(DynamicsInter.class);

        // Complex dynamics in system, sorted by decreasing length
        final List<DynamicsInter> complexes = new ArrayList<DynamicsInter>();

        for (Inter inter : dynamics) {
            DynamicsInter dyn = (DynamicsInter) inter;

            if (dyn.getSymbolString().length() > 1) {
                complexes.add(dyn);
            }
        }

        Collections.sort(
                complexes,
                new Comparator<DynamicsInter>()
        {
            @Override
            public int compare (DynamicsInter d1,
                                DynamicsInter d2)
            {
                // Sort by decreasing length
                return Integer.compare(
                        d2.getSymbolString().length(),
                        d1.getSymbolString().length());
            }
        });

        for (DynamicsInter complex : complexes) {
            complex.swallowShorterDynamics(dynamics);
        }
    }

    //-------------//
    // handleTimes //
    //-------------//
    /**
     * Handle time symbols outside of system header.
     * <p>
     * Isolated time symbols found outside of system header lead to the retrieval of a column of
     * time signatures.
     */
    private void handleTimes ()
    {
        // Retrieve all time symbols (outside staff headers)
        List<Inter> systemTimes = sig.inters(
                new Class[]{TimeWholeInter.class, // Whole symbol like C or 6/8
                            TimeNumberInter.class}); // Partial symbol like 6 or 8
        List<Inter> headerTimes = new ArrayList<Inter>();

        for (Inter inter : systemTimes) {
            Staff staff = inter.getStaff();

            if (inter.getCenter().x < staff.getHeaderStop()) {
                headerTimes.add(inter);
            }
        }

        systemTimes.removeAll(headerTimes);

        if (systemTimes.isEmpty()) {
            return;
        }

        // Dispatch these time symbols into their containing stack
        Map<MeasureStack, Set<Inter>> timeMap = new TreeMap<MeasureStack, Set<Inter>>(
                new Comparator<MeasureStack>()
        {
            @Override
            public int compare (MeasureStack s1,
                                MeasureStack s2)
            {
                return Integer.compare(s1.getIdValue(), s2.getIdValue());
            }
        });

        for (Inter inter : systemTimes) {
            MeasureStack stack = system.getStackAt(inter.getCenter());
            Set<Inter> stackSet = timeMap.get(stack);

            if (stackSet == null) {
                timeMap.put(stack, stackSet = new LinkedHashSet<Inter>());
            }

            stackSet.add(inter);
        }

        // Finally, scan each stack populated with some time sig(s)
        for (Entry<MeasureStack, Set<Inter>> entry : timeMap.entrySet()) {
            MeasureStack stack = entry.getKey();
            TimeBuilder.BasicColumn column = new TimeBuilder.BasicColumn(stack, entry.getValue());
            int res = column.retrieveTime();

            // If the stack does have a validated time sig, discard overlapping stuff right now!
            if (res != -1) {
                final Collection<AbstractTimeInter> times = column.getTimeInters().values();
                final Rectangle columnBox = Inters.getBounds(times);
                List<Inter> neighbors = sig.inters(
                        new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return inter.getBounds().intersects(columnBox)
                               && !(inter instanceof InterEnsemble);
                    }
                });

                neighbors.removeAll(times);

                for (AbstractTimeInter time : times) {
                    for (Iterator<Inter> it = neighbors.iterator(); it.hasNext();) {
                        Inter neighbor = it.next();

                        try {
                            if (neighbor.overlaps(time)) {
                                logger.debug("Deleting time overlapping {}", neighbor);
                                neighbor.remove();
                                it.remove();
                            }
                        } catch (DeletedInterException ignored) {
                        }
                    }
                }
            }
        }
    }
}
