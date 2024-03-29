//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M e a s u r e S t a c k                                    //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code MeasureStack} represents a vertical stack of {@link Measure} instances,
 * embracing all parts of a system.
 * <p>
 * This approach is convenient to use vertical alignments when dealing with time slots.
 * <p>
 * How is measure ID handled:
 * <ol>
 * <li>IDs are recorded as page-based integer values.</li>
 * <li>Initial values start from 1 and are incremented for each following raw measure.</li>
 * <li>Once measure role is refined, its ID may change:
 * <ul>
 * <li>A pickup measure has ID 0, instead of 1.</li>
 * <li>A special value (Xn) is used for second half repeats, when first half repeat is n.</li>
 * <li>Courtesy measures that may be found at end of system (containing just cautionary key or time
 * signatures) have an ID with C suffix (nC).</li>
 * </ul>
 * <li>IDs, as exported in MusicXML, combine the page-based IDs to provide score-based (absolute)
 * IDs.</li>
 * </ol>
 *
 * @see Measure
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "stack")
public class MeasureStack
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MeasureStack.class);

    /** String prefix for a second half id: {@value}. */
    public static final String SECOND_HALF_PREFIX = "X";

    /** String suffix for a cautionary id: {@value}. */
    public static final String CAUTIONARY_SUFFIX = "C";

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * All special kinds of measures.
     */
    public enum Special
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        PICKUP,
        FIRST_HALF,
        SECOND_HALF,
        CAUTIONARY;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** (Page-based) measure Id. */
    @XmlAttribute
    private Integer id;

    /** Minimum left abscissa, across all staves. */
    @XmlAttribute
    private int left;

    /** Maximum right abscissa, across all staves. */
    @XmlAttribute
    private int right;

    /** Sequence of time slots within the measure, from left to right. */
    @XmlElementRef
    private final List<Slot> slots = new ArrayList<Slot>();

    /** Indication for special measure stack. */
    @XmlAttribute
    private Special special;

    /** Repeat sign on either side of the measure stack. */
    @XmlList
    @XmlAttribute(name = "repeat")
    private Set<HorizontalSide> repeats;

    /** Theoretical measure stack duration, based on current time signature. */
    @XmlAttribute(name = "expected")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational expectedDuration;

    /**
     * Actual measure stack duration, based on durations of contained chords.
     * If the stack contains no note (head or rest), actual duration is ZERO.
     * If the stack contains only whole rest(s), actual duration is given by current time signature.
     * Otherwise, duration is computed from contained slots and voices.
     * If stack timing fails for whatever reason, actual duration may be left as null.
     */
    @XmlAttribute(name = "duration")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational actualDuration;

    /** Excess measure stack duration, if any. */
    @XmlAttribute(name = "excess")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational excess;

    /** Anomaly detected, if any. */
    @XmlAttribute(name = "abnormal")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean abnormal;

    // Transient data
    //---------------
    //
    /** The containing system. */
    @Navigable(false)
    private SystemInfo system;

    /** Vertical sequence of (Part) measures, from top to bottom. */
    private final List<Measure> measures = new ArrayList<Measure>();

    /** Unassigned tuplets within stack. */
    private final Set<TupletInter> stackTuplets = new LinkedHashSet<TupletInter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MeasureStack} object.
     *
     * @param system containing system
     */
    public MeasureStack (SystemInfo system)
    {
        this.system = system;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private MeasureStack ()
    {
        this.system = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addInter //
    //----------//
    public void addInter (Inter inter)
    {
        final Part part = inter.getPart();

        if (part != null) {
            Measure measure = getMeasureAt(part);
            measure.addInter(inter);

            if (inter instanceof TupletInter) {
                stackTuplets.remove((TupletInter) inter);
            }
        } else if (inter instanceof TupletInter) {
            stackTuplets.add((TupletInter) inter);
        } else {
            logger.debug("No part yet for {}", inter);
        }
    }

    //------------//
    // addMeasure //
    //------------//
    public void addMeasure (Measure measure)
    {
        if (measures.isEmpty()) {
            left = Integer.MAX_VALUE;
            right = 0;
        }

        measures.add(measure);

        for (Staff staff : measure.getPart().getStaves()) {
            left = Math.min(left, measure.getAbscissa(LEFT, staff));
            right = Math.max(right, measure.getAbscissa(RIGHT, staff));
        }
    }

    //-----------//
    // addRepeat //
    //-----------//
    /**
     * Add a repeat indication on the provided horizontal side.
     *
     * @param side provided side of the measure
     */
    public void addRepeat (HorizontalSide side)
    {
        if (repeats == null) {
            repeats = EnumSet.noneOf(HorizontalSide.class);
        }

        repeats.add(side);
    }

    //------------------//
    // addTimeSignature //
    //------------------//
    public void addTimeSignature (AbstractTimeInter ts)
    {
        // Populate (part) measure with provided time signature
        Point center = ts.getCenter();
        Staff staff = ts.getStaff();

        if (staff == null) {
            ts.setStaff(staff = system.getStaffAtOrAbove(center));
        }

        Measure measure = staff.getPart().getMeasureAt(center);
        measure.addInter(ts);
    }

    //-------------//
    // afterReload //
    //-------------//
    public void afterReload (SystemInfo system)
    {
        try {
            this.system = system;

            // Fill measures
            final int im = system.getStacks().indexOf(this);

            for (Part part : system.getParts()) {
                Measure measure = part.getMeasures().get(im);
                measure.setStack(this);
                measures.add(measure);
            }

            // Forward to slots
            for (Slot slot : slots) {
                slot.afterReload();
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration as computed in this measure from its contained voices,
     * compared to its theoretical duration.
     */
    public void checkDuration ()
    {
        // Check duration of each voice
        for (Measure measure : measures) {
            for (Voice voice : measure.getVoices()) {
                voice.checkDuration(this);
            }
        }
    }

    //------------//
    // clearFrats //
    //------------//
    /**
     * Get rid of all FRAT inters (both good and poor) in this stack, before a new
     * configuration is installed.
     */
    public void clearFrats ()
    {
        for (Measure measure : measures) {
            measure.clearFrats();
        }

        stackTuplets.clear();
    }

    //----------------//
    // computeRepeats //
    //----------------//
    /**
     * Compute the possible repeat indications (left, right) for this stack.
     */
    public void computeRepeats ()
    {
        repeats = null;

        Measure measure = getFirstMeasure();
        PartBarline partBarline = measure.getPartBarlineOn(HorizontalSide.LEFT);

        if (partBarline != null) {
            StaffBarlineInter staffBarline = partBarline.getStaffBarlines().get(0);

            if (staffBarline.isLeftRepeat()) {
                addRepeat(HorizontalSide.LEFT);
            }
        }

        partBarline = measure.getMidPartBarline();

        if (partBarline != null) {
            StaffBarlineInter staffBarline = partBarline.getStaffBarlines().get(0);

            if (staffBarline.isLeftRepeat()) {
                addRepeat(HorizontalSide.LEFT);
            }
        }

        partBarline = measure.getRightPartBarline();

        if (partBarline != null) {
            StaffBarlineInter staffBarline = partBarline.getStaffBarlines().get(0);

            if (staffBarline.isRightRepeat()) {
                addRepeat(HorizontalSide.RIGHT);
            }
        }
    }

    //-------------------//
    // connectTiedVoices //
    //-------------------//
    /**
     * Make sure that notes tied across measures stacks keep the same voice.
     * This is performed for all ties in this part.
     */
    public void connectTiedVoices ()
    {
        //        for (TreeNode tn : getSlurs()) {
        //            Slur slur = (Slur) tn;
        //
        //            if (!slur.isTie()) {
        //                continue;
        //            }
        //
        //            // Voice on left (perhaps in a previous measure / system / page)
        //            Note leftNote = slur.getLeftNote();
        //
        //            if (leftNote == null) {
        //                Slur leftExtension = slur.getLeftExtension();
        //
        //                if (leftExtension == null) {
        //                    continue;
        //                }
        //
        //                leftNote = leftExtension.getLeftNote();
        //
        //                if (leftNote == null) {
        //                    continue;
        //                }
        //            }
        //
        //            AbstractChordInter leftChord = leftNote.getChord();
        //            OldVoice leftVoice = leftChord.getVoice();
        //
        //            // Voice on right
        //            Note rightNote = slur.getRightNote();
        //
        //            if (rightNote == null) {
        //                continue;
        //            }
        //
        //            AbstractChordInter rightChord = rightNote.getChord();
        //            OldVoice rightVoice = rightChord.getVoice();
        //
        //            if (leftVoice.getId() != rightVoice.getId()) {
        //                logger.debug("Tie to map {} and {}", leftChord, rightChord);
        //                rightChord.getMeasure().swapVoiceId(rightVoice, leftVoice.getId());
        //            }
        //        }
    }

    //----------//
    // contains //
    //----------//
    public boolean contains (Point2D point)
    {
        return system.getStackAt(point) == this;
    }

    //--------//
    // filter //
    //--------//
    /**
     * From the provided list of system inters, keep only the ones that are located
     * within this measure stack.
     *
     * @param systemInters the provided list of inters at system level
     * @return the inters kept
     */
    public List<Inter> filter (Collection<Inter> systemInters)
    {
        List<Inter> kept = new ArrayList<Inter>();

        for (Inter inter : systemInters) {
            Point center = inter.getCenter();

            // Rough abscissa limits
            if ((center.x < left) || (center.x > right)) {
                continue;
            }

            Staff staff = inter.getStaff();
            final Measure measure;

            if (staff != null) {
                Part part = staff.getPart();
                measure = part.getMeasureAt(center);
            } else {
                List<Staff> stavesAround = system.getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);
                measure = getMeasureAt(staff);
            }

            // Precise abscissa limits
            if ((measure.getAbscissa(LEFT, staff) <= center.x)
                && (center.x <= measure.getAbscissa(RIGHT, staff))) {
                kept.add(inter);
            }
        }

        return kept;
    }

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the duration of this measure stack , as computed from its contained voices.
     *
     * @return the (actual) measure stack duration, 0 if no rest / note exists in this stack,
     *         null if it could not be computed.
     */
    public Rational getActualDuration ()
    {
        return actualDuration;
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * Among a provided Chord collection, report the chord at smallest euclidian
     * distance from the provided point.
     *
     * @param chords the collection of chords to browse
     * @param point  the reference point
     * @return the euclidian-wise closest chord
     */
    public AbstractChordInter getClosestChord (Collection<AbstractChordInter> chords,
                                               Point2D point)
    {
        AbstractChordInter bestChord = null;
        double bestDsq = Double.MAX_VALUE;

        for (AbstractChordInter chord : chords) {
            Rectangle chordBox = chord.getBounds();
            double dsq = GeoUtil.ptDistanceSq(chordBox, point.getX(), point.getY());

            if (dsq < bestDsq) {
                bestDsq = dsq;
                bestChord = chord;
            }
        }

        return bestChord;
    }

    //----------------//
    // getClosestSlot //
    //----------------//
    /**
     * Report the time slot which has the closest abscissa to a provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest slot
     */
    public Slot getClosestSlot (Point2D point)
    {
        final double xOffset = getXOffset(point);
        Slot bestSlot = null;
        double bestDx = Double.MAX_VALUE;

        for (Slot slot : slots) {
            double dx = Math.abs(slot.getXOffset() - xOffset);

            if (dx < bestDx) {
                bestDx = dx;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    //-------------------------//
    // getCurrentTimeSignature //
    //-------------------------//
    /**
     * Report the time signature which applies in this stack, whether a time signature
     * actually starts this stack in whatever staff, or whether a time signature was
     * found in a previous stack, even in preceding pages.
     * <p>
     * <b>NOTA</b>This method looks up for time sig in preceding pages as well</p>
     *
     * @return the current time signature, or null if not found at all
     */
    public AbstractTimeInter getCurrentTimeSignature ()
    {
        // Backward from this measure to the beginning of the score
        MeasureStack stack = this;
        Page page = system.getPage();

        while (stack != null) {
            // Check in the measure stack
            AbstractTimeInter ts = stack.getFirstMeasure().getTimeSignature();

            if (ts != null) {
                return ts;
            }

            // Move to preceding measure stack
            stack = stack.getPrecedingInPage();

            //
            //            if (measure == null) {
            //                page = page.getPrecedingInScore();
            //
            //                if (page == null) {
            //                    return null;
            //                } else {
            //                    measure = page.getLastSystem().getLastPart().getLastMeasure();
            //                }
            //            }
        }

        return null; // Not found !!!
    }

    //---------------//
    // getEventChord //
    //---------------//
    /**
     * Retrieve the most suitable chord to connect the event point to.
     *
     * @param point  the system-based location
     * @param xRange required abscissa range, or null
     * @return the most suitable chord, or null
     */
    public AbstractChordInter getEventChord (Point2D point,
                                             Rectangle xRange)
    {
        // First, try staff just above
        AbstractChordInter above = getStandardChordAbove(point, xRange);

        if (above != null) {
            return above;
        }

        // Second, try staff just below
        return getStandardChordBelow(point, xRange);
    }

    //-----------//
    // getExcess //
    //-----------//
    /**
     * Report the excess duration of this stack, if any.
     *
     * @return the duration in excess, or null
     */
    public Rational getExcess ()
    {
        return excess;
    }

    //---------------------//
    // getExpectedDuration //
    //---------------------//
    /**
     * Report the theoretical duration of this stack.
     *
     * @return the expected measure stack duration
     */
    public Rational getExpectedDuration ()
    {
        return expectedDuration;

        //        try {
        //            if (expectedDuration == null) {
        //                int numerator;
        //                int denominator;
        //                AbstractTimeInter ts = getCurrentTimeSignature();
        //
        //                if (ts != null) {
        //                    numerator = ts.getNumerator();
        //                    denominator = ts.getDenominator();
        //                } else {
        //                    numerator = 4;
        //                    denominator = 4;
        //                }
        //
        //                expectedDuration = new Rational(numerator, denominator);
        //            }
        //
        //            return expectedDuration;
        //        } catch (NullPointerException npe) {
        //            throw new TimeSignature.InvalidTimeSignature();
        //        }
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    public Measure getFirstMeasure ()
    {
        if (measures.isEmpty()) {
            return null;
        }

        return measures.get(0);
    }

    //--------------------//
    // getFollowingInPage //
    //--------------------//
    /**
     * Report the following measure stack of this one, either in this system, or in the
     * following system, but still in the same page.
     *
     * @return the following measure stack, or null if not found in the page
     */
    public MeasureStack getFollowingInPage ()
    {
        // Look in current part
        MeasureStack nextStack = getNextSibling();

        if (nextStack != null) {
            return nextStack;
        }

        SystemInfo nextSystem = system.getFollowingInPage();

        if (nextSystem != null) {
            return nextSystem.getFirstStack();
        } else {
            return null;
        }
    }

    //---------------//
    // getHeadChords //
    //---------------//
    public Set<HeadChordInter> getHeadChords ()
    {
        Set<HeadChordInter> headChords = new LinkedHashSet<HeadChordInter>();

        for (Measure measure : measures) {
            headChords.addAll(measure.getHeadChords());
        }

        return headChords;
    }

    //------------//
    // getIdValue //
    //------------//
    /**
     * Report the numeric value of the measure id.
     * Note that first (n) and second (Xn) measure halves share the same numeric value n.
     *
     * @return the numeric value of measure id
     */
    public int getIdValue ()
    {
        return id;
    }

    //-------------//
    // getLastSlot //
    //-------------//
    /**
     * Report the last slot in stack.
     *
     * @return the last slot or null if none
     */
    public Slot getLastSlot ()
    {
        if (slots.isEmpty()) {
            return null;
        }

        return slots.get(slots.size() - 1);
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * @return the left
     */
    public int getLeft ()
    {
        return left;
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure at provided staff
     *
     * @param staff the provided staff
     * @return the measure that contains the provided staff
     */
    public Measure getMeasureAt (Staff staff)
    {
        for (Measure measure : measures) {
            if (measure.getPart().getStaves().contains(staff)) {
                return measure;
            }
        }

        return null;
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure at provided part
     *
     * @param part the provided part
     * @return the measure that contains the provided part
     */
    public Measure getMeasureAt (Part part)
    {
        for (Measure measure : measures) {
            if (measure.getPart() == part) {
                return measure;
            }
        }

        return null;
    }

    //-------------//
    // getMeasures //
    //-------------//
    public List<Measure> getMeasures ()
    {
        return measures;
    }

    //----------------//
    // getNextSibling //
    //----------------//
    /**
     * Return the next measure stack within the same system.
     *
     * @return previous sibling measure stack in system, or null
     */
    public MeasureStack getNextSibling ()
    {
        final List<MeasureStack> stacks = system.getStacks();
        final int index = stacks.indexOf(this);

        if (index < (stacks.size() - 1)) {
            return stacks.get(index + 1);
        }

        return null;
    }

    //-----------//
    // getPageId //
    //-----------//
    /**
     * Report the page-based measure id string.
     *
     * @return the page-based measure id string
     */
    public String getPageId ()
    {
        if (id != null) {
            return ((special == Special.SECOND_HALF) ? SECOND_HALF_PREFIX : "") + id
                   + (isCautionary() ? CAUTIONARY_SUFFIX : "");
        }

        // No id defined yet
        StringBuilder sb = new StringBuilder();
        sb.append("S").append(system.getId());
        sb.append("M").append(1 + system.getStacks().indexOf(this));

        return sb.toString();
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the preceding measure stack of this one, either in this system, or in the
     * preceding system, but still in the same page.
     *
     * @return the preceding measure stack, or null if not found in the page
     */
    public MeasureStack getPrecedingInPage ()
    {
        // Look in current part
        MeasureStack prevStack = getPreviousSibling();

        if (prevStack != null) {
            return prevStack;
        }

        SystemInfo precedingSystem = system.getPrecedingInPage();

        if (precedingSystem != null) {
            return precedingSystem.getLastStack();
        } else {
            return null;
        }
    }

    //--------------------//
    // getPreviousSibling //
    //--------------------//
    /**
     * Return the preceding measure stack within the same system.
     *
     * @return previous sibling measure stack in system, or null
     */
    public MeasureStack getPreviousSibling ()
    {
        final List<MeasureStack> stacks = system.getStacks();
        final int index = stacks.indexOf(this);

        if (index > 0) {
            return stacks.get(index - 1);
        }

        return null;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * @return the right
     */
    public int getRight ()
    {
        return right;
    }

    //------------//
    // getScoreId //
    //------------//
    /**
     * Report the character string of the score-based measure id.
     *
     * @param score the containing score
     * @return the (absolute) score-based measure id string
     */
    public String getScoreId (Score score)
    {
        if (id == null) {
            return null;
        }

        final Page page = system.getPage();
        final int pageMeasureIdOffset = score.getMeasureIdOffset(page);

        return ((special == Special.SECOND_HALF) ? SECOND_HALF_PREFIX : "")
               + (pageMeasureIdOffset + id);
    }

    //----------//
    // getSlots //
    //----------//
    /**
     * Report the ordered collection of slots.
     *
     * @return the collection of slots
     */
    public List<Slot> getSlots ()
    {
        return slots;
    }

    //------------------//
    // getSlotsDuration //
    //------------------//
    /**
     * Report the duration for this measure, as computed from content in terms of slots
     * and chords.
     * <p>
     * <b>NOTA</b>: if measure has no slot (case of a whole rest or case of an empty stack),
     * result is ZERO.
     *
     * @return the measure duration as computed on slots and chords
     */
    public Rational getSlotsDuration ()
    {
        Rational measureDur = Rational.ZERO;

        // Whole/multi rests are handled outside of slots
        for (Slot slot : slots) {
            if (slot.getTimeOffset() != null) {
                for (AbstractChordInter chord : slot.getChords()) {
                    Rational chordEnd = slot.getTimeOffset().plus(chord.getDuration());

                    if (chordEnd.compareTo(measureDur) > 0) {
                        measureDur = chordEnd;
                    }
                }
            }
        }

        return measureDur;
    }

    //-----------------------//
    // getStandardChordAbove //
    //-----------------------//
    /**
     * Retrieve the closest chord (head or rest) within staff above.
     *
     * @param point  the system-based location
     * @param xRange required abscissa range, or null
     * @return the most suitable chord, or null
     */
    public AbstractChordInter getStandardChordAbove (Point2D point,
                                                     Rectangle xRange)
    {
        Collection<AbstractChordInter> aboves = getStandardChordsAbove(point, xRange);

        if (!aboves.isEmpty()) {
            return getClosestChord(aboves, point);
        }

        return null;
    }

    //-----------------------//
    // getStandardChordBelow //
    //-----------------------//
    /**
     * Retrieve the closest chord (head or rest) within staff below.
     *
     * @param point  the system-based location
     * @param xRange required abscissa range, or null
     * @return the most suitable chord, or null
     */
    public AbstractChordInter getStandardChordBelow (Point2D point,
                                                     Rectangle xRange)
    {
        Collection<AbstractChordInter> belows = getStandardChordsBelow(point, xRange);

        if (!belows.isEmpty()) {
            return getClosestChord(belows, point);
        }

        return null;
    }

    //-------------------//
    // getStandardChords //
    //-------------------//
    public Set<AbstractChordInter> getStandardChords ()
    {
        Set<AbstractChordInter> stdChords = new LinkedHashSet<AbstractChordInter>();

        for (Measure measure : measures) {
            stdChords.addAll(measure.getStandardChords());
        }

        return stdChords;
    }

    //------------------------//
    // getStandardChordsAbove //
    //------------------------//
    /**
     * Report the set of standard chords whose 'head' is located in the staff above the
     * provided point.
     *
     * @param point  the provided point
     * @param xRange required abscissa range, or null
     * @return the (perhaps empty) set of chords
     */
    public Set<AbstractChordInter> getStandardChordsAbove (Point2D point,
                                                           Rectangle xRange)
    {
        Staff desiredStaff = getSystem().getStaffAtOrAbove(point);
        Set<AbstractChordInter> found = new LinkedHashSet<AbstractChordInter>();
        Measure measure = getMeasureAt(desiredStaff);

        if (measure != null) {
            for (AbstractChordInter chord : measure.getStandardChords()) {
                if (chord.getBottomStaff() == desiredStaff) {
                    if ((xRange == null) || (GeoUtil.xOverlap(chord.getBounds(), xRange) > 0)) {
                        Point head = chord.getHeadLocation();

                        if ((head != null) && (head.y < point.getY())) {
                            found.add(chord);
                        }
                    }
                }
            }
        }

        return found;
    }

    //------------------------//
    // getStandardChordsBelow //
    //------------------------//
    /**
     * Report the set of standard chords whose 'head' is located in the staff below the
     * provided point.
     *
     * @param point  the provided point
     * @param xRange required abscissa range, or null
     * @return the (perhaps empty) collection of chords
     */
    public Set<AbstractChordInter> getStandardChordsBelow (Point2D point,
                                                           Rectangle xRange)
    {
        Staff desiredStaff = getSystem().getStaffAtOrBelow(point);
        Set<AbstractChordInter> found = new LinkedHashSet<AbstractChordInter>();
        Measure measure = getMeasureAt(desiredStaff);

        if (measure != null) {
            for (AbstractChordInter chord : measure.getStandardChords()) {
                if (chord.getTopStaff() == desiredStaff) {
                    if ((xRange == null) || (GeoUtil.xOverlap(chord.getBounds(), xRange) > 0)) {
                        Point head = chord.getHeadLocation();

                        if ((head != null) && (head.y > point.getY())) {
                            found.add(chord);
                        }
                    }
                }
            }
        }

        return found;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * @return the system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this stack (whatever the staff).
     *
     * @return the stack time signature, or null if not found
     */
    public AbstractTimeInter getTimeSignature ()
    {
        for (Measure measure : measures) {
            AbstractTimeInter ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }
        }

        return null; // Not found
    }

    //------------//
    // getTuplets //
    //------------//
    /**
     * Report all tuplet candidates within the stack.
     * <p>
     * A containing part cannot be safely assigned to a tuplet before its embraced chords are known,
     * therefore tuplet candidates may be kept in stack for a while before being dispatched to their
     * precise measure.
     *
     * @return all tuplet candidates
     */
    public Set<TupletInter> getTuplets ()
    {
        Set<TupletInter> all = new LinkedHashSet<TupletInter>();

        for (Measure measure : measures) {
            all.addAll(measure.getTuplets());
        }

        // Add the non-measure-assigned stackTuplets
        all.addAll(stackTuplets);

        return all;
    }

    //-----------//
    // getVoices //
    //-----------//
    public List<Voice> getVoices ()
    {
        List<Voice> stackVoices = new ArrayList<Voice>();

        for (Measure measure : measures) {
            stackVoices.addAll(measure.getVoices());
        }

        return Collections.unmodifiableList(stackVoices);
    }

    //--------------------//
    // getWholeRestChords //
    //--------------------//
    public Set<AbstractChordInter> getWholeRestChords ()
    {
        final Set<AbstractChordInter> set = new LinkedHashSet<AbstractChordInter>();

        for (Measure measure : measures) {
            for (RestChordInter chord : measure.getRestChords()) {
                final List<Inter> members = chord.getMembers();

                if (!members.isEmpty() && (members.get(0).getShape() == Shape.WHOLE_REST)) {
                    set.add(chord);
                }
            }
        }

        return set;
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the abscissa offset since stack left border of the provided point.
     *
     * @param point the provided point
     * @return xOffset of the point WRT stack start
     */
    public double getXOffset (Point2D point)
    {
        return getXOffset(point, system.getStavesAround(point));
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the precise abscissa offset since stack left border of the provided point.
     *
     * @param point the provided point
     * @param staff the staff of reference
     * @return xOffset of the point WRT stack left side
     */
    public double getXOffset (Point2D point,
                              Staff staff)
    {
        // Extrapolate, using skew, from single staff
        final Skew skew = system.getSkew();
        final Measure measure = getMeasureAt(staff);
        final Point left = measure.getSidePoint(HorizontalSide.LEFT, staff);

        return skew.deskewed(point).getX() - skew.deskewed(left).getX();
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the precise abscissa offset since stack left border of the provided point.
     *
     * @param point        the provided point
     * @param stavesAround one or two staves that surround the provided point
     * @return xOffset of the point WRT stack left side
     */
    public double getXOffset (Point2D point,
                              List<Staff> stavesAround)
    {
        Staff staff1 = stavesAround.get(0);
        Measure measure1 = getMeasureAt(staff1);
        Point left1 = measure1.getSidePoint(HorizontalSide.LEFT, staff1);

        if (stavesAround.size() > 1) {
            // Interpolate between staff above & staff below
            LineInfo line1 = staff1.getLines().get(staff1.getLines().size() / 2);
            double y1 = line1.yAt(point.getX());
            double offset1 = point.getX() - left1.x;

            Staff staff2 = stavesAround.get(1);
            Measure measure2 = getMeasureAt(staff2);
            Point left2 = measure2.getSidePoint(HorizontalSide.LEFT, staff2);
            LineInfo line2 = staff2.getLines().get(staff2.getLines().size() / 2);
            double y2 = line2.yAt(point.getX());
            double offset2 = point.getX() - left2.x;

            return offset1 + (((offset2 - offset1) * (point.getY() - y1)) / (y2 - y1));
        } else {
            return getXOffset(point, staff1);
        }
    }

    //------------//
    // isAbnormal //
    //------------//
    /**
     * Report whether this stack is abnormal.
     *
     * @return the abnormal status
     */
    public boolean isAbnormal ()
    {
        return abnormal;
    }

    //--------------//
    // isCautionary //
    //--------------//
    /**
     * Report whether this measure stack is a cautionary stack (just changes in CKT).
     *
     * @return true if measure is cautionary
     */
    public boolean isCautionary ()
    {
        return special == Special.CAUTIONARY;
    }

    //-------------//
    // isFirstHalf //
    //-------------//
    /**
     * Report whether this measure stack is a repeat first half.
     *
     * @return true if measure is firstHalf
     */
    public boolean isFirstHalf ()
    {
        return special == Special.FIRST_HALF;
    }

    //------------//
    // isImplicit //
    //------------//
    /**
     * Report whether this measure stack is implicit (pickup or repeat last half).
     *
     * @return true if measure is pickup or secondHalf
     */
    public boolean isImplicit ()
    {
        return (special == Special.PICKUP) || (special == Special.SECOND_HALF);
    }

    //----------//
    // isRepeat //
    //----------//
    public boolean isRepeat (HorizontalSide side)
    {
        return (repeats != null) && repeats.contains(side);
    }

    //----------------//
    // mergeWithRight //
    //----------------//
    /**
     * Merge this stack with the content of the following stack on the right.
     *
     * @param rightStack the following measure stack
     */
    public void mergeWithRight (MeasureStack rightStack)
    {
        // Merge the measures, part by part
        for (int ip = 0; ip < rightStack.measures.size(); ip++) {
            measures.get(ip).mergeWithRight(rightStack.measures.get(ip));
        }

        // Merge the stacks data
        right = rightStack.right;

        if (rightStack.actualDuration != null) {
            actualDuration = (actualDuration == null) ? rightStack.actualDuration
                    : actualDuration.plus(rightStack.actualDuration);
        }

        // Merge the repeat info
        if (rightStack.isRepeat(RIGHT)) {
            this.addRepeat(RIGHT);
        }

        // Beware, merged slots must have their stack & xOffset updated accordingly
        slots.addAll(rightStack.slots);

        for (Slot slot : slots) {
            slot.setStack(this);
        }

        //TODO: what about the now "inside" barline (which may have a repeat sign) ???
    }

    //------------//
    // printVoices//
    //------------//
    /**
     * Print the voices of this measure stack on standard output.
     *
     * @param title a potential title for this printout, or null
     */
    public void printVoices (String title)
    {
        try {
            StringBuilder sb = new StringBuilder();

            // Title
            if (title != null) {
                sb.append(title);
            }

            // Measure
            sb.append(this);

            // Slot headers
            if (!slots.isEmpty()) {
                sb.append("\n    ");

                for (Slot slot : slots) {
                    if (slot.getTimeOffset() != null) {
                        sb.append("|").append(String.format("%-8s", slot.getTimeOffset()));
                    }
                }

                sb.append("|").append(getSlotsDuration());
            }

            for (Measure measure : measures) {
                sb.append("\n--- P").append(measure.getPart().getId());

                for (Voice voice : measure.getVoices()) {
                    sb.append("\n").append(voice.toStrip());
                }
            }

            logger.info(sb.toString());
        } catch (Exception ex) {
            logger.warn("Error printing voices for {} {}", this, ex.toString(), ex);
        }
    }

    //-------------//
    // removeInter //
    //-------------//
    public void removeInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP removeInter {} from [}", inter, this);
        }

        final Part part = inter.getPart();

        if (part != null) {
            int partIndex = system.getParts().indexOf(part);
            Measure measure = measures.get(partIndex);
            measure.removeInter(inter);
        }

        if (inter instanceof TupletInter) {
            stackTuplets.remove((TupletInter) inter);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the measure stack area with provided color.
     *
     * @param g     graphics context
     * @param color provided color
     */
    public void render (Graphics2D g,
                        Color color)
    {
        // Save some drawing
        final Rectangle clip = g.getClipBounds();
        final Rectangle systemRect = system.getBounds();

        if ((clip != null) && !clip.intersects(systemRect)) {
            return;
        }

        g.setColor(color);

        Sheet sheet = system.getSheet();

        // Most inters from first measure
        Staff firstStaff = system.getFirstStaff();
        Measure firstMeasure = getMeasureAt(firstStaff);
        LineInfo firstLine = firstStaff.getFirstLine();
        int top = Math.min(firstLine.yAt(left), firstLine.yAt(right));

        for (Inter inter : firstMeasure.getTimingInters()) {
            top = Math.min(top, inter.getBounds().y);
        }

        // Most inters from last measure
        Staff lastStaff = system.getLastStaff();
        Measure lastMeasure = getMeasureAt(lastStaff);
        LineInfo lastLine = lastStaff.getLastLine();
        int bottom = Math.max(lastLine.yAt(left), lastLine.yAt(right));

        for (Inter inter : lastMeasure.getTimingInters()) {
            Rectangle bounds = inter.getBounds();
            bottom = Math.max(bottom, bounds.y + bounds.height);
        }

        // Inters from stack tuplets
        for (Inter inter : stackTuplets) {
            Rectangle bounds = inter.getBounds();
            top = Math.min(top, bounds.y);
            bottom = Math.max(bottom, bounds.y + bounds.height);
        }

        Area area = new Area(new Rectangle(left, 0, right - left + 1, sheet.getHeight()));
        area.intersect(new Area(new Rectangle(0, top, sheet.getWidth(), bottom - top + 1)));
        g.fill(area);
    }

    //-------------//
    // resetRhythm //
    //-------------//
    public void resetRhythm ()
    {
        setAbnormal(false);
        excess = null;
        slots.clear();
        setActualDuration(null);

        // Reset every measure within this stack
        for (Measure measure : measures) {
            measure.resetRhythm();
        }
    }

    //-------------//
    // setAbnormal //
    //-------------//
    /**
     * Mark this stack as being abnormal or not.
     *
     * @param abnormal new value
     */
    public void setAbnormal (boolean abnormal)
    {
        this.abnormal = abnormal;
    }

    //-------------------//
    // setActualDuration //
    //-------------------//
    /**
     * Register in this measure stack its actual duration.
     *
     * @param actualDuration the duration value
     */
    public void setActualDuration (Rational actualDuration)
    {
        this.actualDuration = actualDuration;
    }

    //-----------//
    // setPickup //
    //-----------//
    public void setCautionary ()
    {
        special = Special.CAUTIONARY;
    }

    //-----------//
    // setExcess //
    //-----------//
    /**
     * Assign an excess duration for this stack.
     *
     * @param excess the duration in excess
     */
    public void setExcess (Rational excess)
    {
        this.excess = excess;
        setAbnormal(true);
    }

    //---------------------//
    // setExpectedDuration //
    //---------------------//
    public void setExpectedDuration (Rational expectedDuration)
    {
        this.expectedDuration = expectedDuration;
    }

    //--------------//
    // setFirstHalf //
    //--------------//
    public void setFirstHalf ()
    {
        special = Special.FIRST_HALF;
    }

    //------------//
    // setIdValue //
    //------------//
    /**
     * Assign the proper page-based id value to this measure stack.
     *
     * @param id the proper page-based measure stack id value
     */
    public void setIdValue (int id)
    {
        this.id = id;
    }

    //-----------//
    // setPickup //
    //-----------//
    public void setPickup ()
    {
        special = Special.PICKUP;
    }

    //---------------//
    // setSecondHalf //
    //---------------//
    public void setSecondHalf ()
    {
        special = Special.SECOND_HALF;
    }

    //------------//
    // setSpecial //
    //------------//
    public void setSpecial (Special special)
    {
        this.special = special;
    }

    //---------//
    // shorten //
    //---------//
    /**
     * Flag this measure stack as partial (shorter than expected duration).
     *
     * @param shortening how much the measure stack duration is to be reduced
     */
    public void shorten (Rational shortening)
    {
        //        // Remove any final forward mark consistent with the shortening
        //        for (Voice voice : voices) {
        //            Rational duration = voice.getTermination();
        //
        //            if (duration != null) {
        //                //                if (duration.equals(shortening)) {
        //                //                    if (!voice.isWhole()) {
        //                //                        // Remove the related mark
        //                //                        AbstractChordInter chord = voice.getLastChord();
        //                //
        //                //                        if (chord != null) {
        //                //                            int nbMarks = chord.getMarks().size();
        //                //
        //                //                            if (nbMarks > 0) {
        //                //                                Mark mark = chord.getMarks().get(nbMarks - 1);
        //                //                                logger.debug(
        //                //                                        "{} Removing final forward: {}",
        //                //                                        getContextString(),
        //                //                                        (Rational) mark.getData());
        //                //                                chord.getMarks().remove(mark);
        //                //                            } else {
        //                //                                chord.addError("No final mark to remove in a partial measure");
        //                //
        //                //                                return;
        //                //                            }
        //                //                        } else {
        //                //                            addError("No final chord in " + voice);
        //                //
        //                //                            return;
        //                //                        }
        //                //                    }
        //                //                } else {
        //                //                    addError(
        //                //                            "Non consistent partial measure shortening:" + shortening.opposite() + " "
        //                //                            + voice + ": " + duration.opposite());
        //                //
        //                //                    return;
        //                //                }
        //            }
        //        }
    }

    //----------------//
    // splitAtBarline //
    //----------------//
    /**
     * Split this stack at the provided system-level barlines.
     * <p>
     * We create a new stack on left side of old stack which will become the right stack.
     * We update old (right) stack on its left side.
     *
     * @param systemBarline column of PartBarline, one per system part
     * @return the newly created stack on the left
     */
    public MeasureStack splitAtBarline (List<PartBarline> systemBarline)
    {
        final int stackIndex = system.getStacks().indexOf(this);
        final MeasureStack leftStack = new MeasureStack(system);
        leftStack.left = this.left;
        leftStack.right = 0;
        this.left = Integer.MAX_VALUE;

        final List<Part> systemParts = system.getParts();

        for (int partIndex = 0; partIndex < systemParts.size(); partIndex++) {
            final Part part = systemParts.get(partIndex);
            final PartBarline partBarline = systemBarline.get(partIndex);
            Map<Staff, Integer> xRefs = new HashMap<Staff, Integer>();

            for (Staff staff : part.getStaves()) {
                final int xRef = partBarline.getRightX(part, staff);
                xRefs.put(staff, xRef);
                leftStack.right = Math.max(leftStack.right, xRef);
                this.left = Math.min(this.left, xRef);
            }

            final Measure measure = getMeasureAt(part);
            final Measure leftMeasure = measure.splitAt(xRefs);
            leftMeasure.setStack(leftStack);
            leftMeasure.setRightPartBarline(partBarline);
            leftStack.measures.add(leftMeasure);

            // Insert leftMeasure into part, just before the old (right) measure
            part.addMeasure(stackIndex, leftMeasure);
        }

        // Stack tuplets (not in any measure yet)
        if (!stackTuplets.isEmpty()) {
            for (Iterator<TupletInter> it = stackTuplets.iterator(); it.hasNext();) {
                TupletInter tuplet = it.next();

                if (tuplet.getCenter().x <= leftStack.right) {
                    leftStack.addInter(tuplet);
                    it.remove();
                }
            }
        }

        // Insert leftStack into system, just before this old (right) stack
        system.addStack(stackIndex, leftStack);

        return leftStack;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(getPageId());

        return sb.toString();
    }
}
