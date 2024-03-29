//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d I n t e r                                       //
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
package org.audiveris.omr.sig.inter;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.image.ShapeDescriptor;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.image.TemplateFactory;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.BasicImpacts;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.ByteUtil;
import org.audiveris.omr.util.Corner;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import static org.audiveris.omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code HeadInter} represents a note head, that is any head shape including
 * whole and breve, but not a rest.
 * <p>
 * These rather round-shaped symbols are retrieved via template-matching technique.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head")
@XmlAccessorType(XmlAccessType.NONE)
public class HeadInter
        extends AbstractNoteInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadInter.class);

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Absolute location of head template pivot. */
    @XmlElement
    private final Point pivot;

    /** Relative pivot position WRT head. */
    @XmlAttribute
    private final Anchor anchor;

    // Transient data
    //---------------
    //
    /** Shape template descriptor. */
    private ShapeDescriptor descriptor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadInter} object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param shape   the underlying shape
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public HeadInter (Point pivot,
                      Anchor anchor,
                      Rectangle bounds,
                      Shape shape,
                      GradeImpacts impacts,
                      Staff staff,
                      Double pitch)
    {
        super(null, bounds, shape, impacts, staff, pitch);
        this.pivot = pivot;
        this.anchor = anchor;
    }

    /**
     * Creates a new {@code HeadInter} object.
     *
     * @param pivot  the template pivot
     * @param anchor relative pivot configuration
     * @param bounds the object bounds
     * @param shape  the underlying shape
     * @param grade  quality grade
     * @param staff  the related staff
     * @param pitch  the note pitch
     */
    public HeadInter (Point pivot,
                      Anchor anchor,
                      Rectangle bounds,
                      Shape shape,
                      double grade,
                      Staff staff,
                      Double pitch)
    {
        super(null, bounds, shape, grade, staff, pitch);
        this.pivot = pivot;
        this.anchor = anchor;
    }

    /** No-arg constructor needed by JAXB. */
    private HeadInter ()
    {
        this.pivot = null;
        this.anchor = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        if (ShapeSet.StemHeads.contains(shape)) {
            setAbnormal(true); // No stem linked yet
        }
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        if (ShapeSet.StemHeads.contains(shape)) {
            // Check if a stem is connected
            setAbnormal(!sig.hasRelation(this, HeadStemRelation.class));
        }

        return isAbnormal();
    }

    //-----------//
    // duplicate //
    //-----------//
    public HeadInter duplicate ()
    {
        return duplicateAs(shape);
    }

    //-------------//
    // duplicateAs //
    //-------------//
    public HeadInter duplicateAs (Shape shape)
    {
        HeadInter clone = new HeadInter(pivot, anchor, bounds, shape, impacts, staff, pitch);
        clone.setGlyph(this.glyph);
        clone.setMirror(this);

        if (impacts == null) {
            clone.setGrade(this.grade);
        }

        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }

    //---------------//
    // getAccidental //
    //---------------//
    /**
     * Report the (local) accidental, if any, related to this head.
     *
     * @return the related accidental, or null
     */
    public AlterInter getAccidental ()
    {
        for (Relation rel : sig.getRelations(this, AlterHeadRelation.class)) {
            return (AlterInter) sig.getOppositeInter(this, rel);
        }

        return null;
    }

    //----------//
    // getAlter //
    //----------//
    /**
     * Report the actual alteration of this note, taking into account the accidental of
     * this note if any, the accidental of previous note with same step within the same
     * measure, a tie from previous measure and finally the current key signature.
     *
     * @param fifths fifths value for current key signature
     * @return the actual alteration
     */
    public int getAlter (Integer fifths)
    {
        return getAlter(fifths, true);
    }

    //----------//
    // getChord //
    //----------//
    @Override
    public HeadChordInter getChord ()
    {
        return (HeadChordInter) getEnsemble();
    }

    //---------------//
    // getCoreBounds //
    //---------------//
    @Override
    public Rectangle2D getCoreBounds ()
    {
        return shrink(getBounds());
    }

    //---------------//
    // getDescriptor //
    //---------------//
    public ShapeDescriptor getDescriptor ()
    {
        if (descriptor == null) {
            final int pointSize = staff.getHeadPointSize();
            descriptor = TemplateFactory.getInstance().getCatalog(pointSize).getDescriptor(shape);
        }

        return descriptor;
    }

    //--------------------//
    // getShrinkHoriRatio //
    //--------------------//
    public static double getShrinkHoriRatio ()
    {
        return constants.shrinkHoriRatio.getValue();
    }

    //--------------------//
    // getShrinkVertRatio //
    //--------------------//
    public static double getShrinkVertRatio ()
    {
        return constants.shrinkVertRatio.getValue();
    }

    //--------------//
    // getSideStems //
    //--------------//
    /**
     * Report the stems linked to this head, organized by head side.
     *
     * @return the map of linked stems, organized by head side
     * @see #getStems()
     */
    public Map<HorizontalSide, Set<StemInter>> getSideStems ()
    {
        // Split connected stems into left and right sides
        final Map<HorizontalSide, Set<StemInter>> map = new EnumMap<HorizontalSide, Set<StemInter>>(
                HorizontalSide.class);

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            HeadStemRelation rel = (HeadStemRelation) relation;
            HorizontalSide side = rel.getHeadSide();
            Set<StemInter> set = map.get(side);

            if (set == null) {
                map.put(side, set = new LinkedHashSet<StemInter>());
            }

            set.add((StemInter) sig.getEdgeTarget(rel));
        }

        return map;
    }

    //-----------------------//
    // getStemReferencePoint //
    //-----------------------//
    /**
     * Report the reference point for a stem connection.
     *
     * @param anchor    desired side for stem (typically TOP_RIGHT_STEM or BOTTOM_LEFT_STEM)
     * @param interline relevant interline value
     * @return the reference point
     */
    public Point2D getStemReferencePoint (Anchor anchor,
                                          int interline)
    {
        ShapeDescriptor desc = getDescriptor();
        Rectangle templateBox = desc.getBounds(this.getBounds());
        Point ref = templateBox.getLocation();
        Point offset = desc.getOffset(anchor);
        ref.translate(offset.x, offset.y);

        return ref;
    }

    //----------//
    // getStems //
    //----------//
    /**
     * Report the stems linked to this head, whatever the side.
     *
     * @return set of linked stems
     * @see #getSideStems()
     */
    public Set<StemInter> getStems ()
    {
        final Set<StemInter> set = new LinkedHashSet<StemInter>();

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            set.add((StemInter) sig.getEdgeTarget(relation));
        }

        return set;
    }

    //----------------//
    // haveSameHeight //
    //----------------//
    /**
     * Check whether two heads represent the same height
     * (same octave, same step, same alteration).
     *
     * @param h1 first head
     * @param h2 second head, down the score
     * @return true if the heads are equivalent.
     */
    public static boolean haveSameHeight (HeadInter h1,
                                          HeadInter h2)
    {
        if ((h1 == null) || (h2 == null)) {
            return false;
        }

        // Step
        if (h1.getStep() != h2.getStep()) {
            return false;
        }

        // Octave
        if (h1.getOctave() != h2.getOctave()) {
            return false;
        }

        // Alteration
        Staff s1 = h1.getStaff();
        Measure m1 = s1.getPart().getMeasureAt(h1.getCenter());
        KeyInter k1 = m1.getKeyBefore(s1);
        int f1 = (k1 != null) ? k1.getFifths() : 0;

        Staff s2 = h2.getStaff();
        Measure m2 = s2.getPart().getMeasureAt(h2.getCenter());
        KeyInter k2 = m2.getKeyBefore(s2);
        int f2 = (k2 != null) ? k2.getFifths() : 0;

        if (m1 == m2) {
            // Both heads are in same measure
            return (f1 == f2) && (h1.getStaff() == h2.staff);
        } else {
            int a1 = h1.getAlter(f1, false);
            int a2 = h2.getAlter(f2, false);

            return a1 == a2;
        }
    }

    //----------//
    // overlaps //
    //----------//
    /**
     * Precise overlap implementation between notes, based on their pitch value.
     * <p>
     * TODO: A clean overlap check might use true distance tables around each of the heads.
     * For the time being, we simply play with the width and area of intersection rectangle.
     *
     * @param that another inter (perhaps a note)
     * @return true if overlap is detected
     * @throws DeletedInterException when an Inter instance no longer exists in its SIG
     */
    @Override
    public boolean overlaps (Inter that)
            throws DeletedInterException
    {
        // Specific between notes
        if (that instanceof HeadInter) {
            if (this.isVip() && ((HeadInter) that).isVip()) {
                //logger.info("AbstractHeadInter checking overlaps between {} and {}", this, that);
            }

            HeadInter thatHead = (HeadInter) that;

            // Check vertical distance
            if (this.getStaff() == that.getStaff()) {
                if (Math.abs(thatHead.getIntegerPitch() - getIntegerPitch()) > 1) {
                    return false;
                }
            } else {
                // We have two note heads from different staves and with overlapping bounds!
                fixDuplicateWith(thatHead); // Throws DeletedInterException when fixed

                return true;
            }

            // Check horizontal distance
            Rectangle thisBounds = this.getBounds();
            Rectangle thatBounds = thatHead.getBounds();
            Rectangle common = thisBounds.intersection(thatBounds);

            if (common.width <= 0) {
                return false;
            }

            int thisArea = thisBounds.width * thisBounds.height;
            int thatArea = thatBounds.width * thatBounds.height;
            int minArea = Math.min(thisArea, thatArea);
            int commonArea = common.width * common.height;
            double areaRatio = (double) commonArea / minArea;
            boolean res = (common.width > (constants.maxOverlapDxRatio.getValue() * thisBounds.width))
                          && (areaRatio > constants.maxOverlapAreaRatio.getValue());

            return res;

            //        } else if (that instanceof StemInter) {
            //            // Head with respect to a stem
            //            // First, standard check
            //            if (!Glyphs.intersect(this.getGlyph(), that.getGlyph(), false)) {
            //                return false;
            //            }
            //
            //            // Second, limit stem vertical range to connection points of ending heads if any
            //            // (Assuming wrong-side ending heads have been pruned beforehand)
            //            StemInter stem = (StemInter) that;
            //            Line2D line = stem.computeAnchoredLine();
            //            int top = (int) Math.ceil(line.getY1());
            //            int bottom = (int) Math.floor(line.getY2());
            //            Rectangle box = stem.getBounds();
            //            Rectangle anchorRect = new Rectangle(box.x, top, box.width, bottom - top + 1);
            //
            //            return this.getCoreBounds().intersects(anchorRect);
        }

        // Basic test
        return super.overlaps(that);
    }

    //---------------//
    // retrieveGlyph //
    //---------------//
    /**
     * Use descriptor to build an underlying glyph.
     *
     * @param image the image to read pixels from
     * @return the underlying glyph or null if failed
     */
    public Glyph retrieveGlyph (ByteProcessor image)
    {
        getDescriptor();

        final Sheet sheet = staff.getSystem().getSheet();
        final Template tpl = descriptor.getTemplate();
        final Rectangle interBox = getBounds();
        final Rectangle descBox = descriptor.getBounds(interBox);

        // Foreground points (coordinates WRT descBox)
        final List<Point> fores = tpl.getForegroundPixels(descBox, image);

        if (fores.isEmpty()) {
            logger.info("No foreground pixels for {}", this);

            return null;
        }

        final Rectangle foreBox = PointUtil.boundsOf(fores);

        final ByteProcessor buf = new ByteProcessor(foreBox.width, foreBox.height);
        ByteUtil.raz(buf);

        for (Point p : fores) {
            buf.set(p.x - foreBox.x, p.y - foreBox.y, 0);
        }

        // Runs
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);

        // Glyph
        glyph = sheet.getGlyphIndex().registerOriginal(
                new BasicGlyph(descBox.x + foreBox.x, descBox.y + foreBox.y, runTable));

        // Use glyph bounds as inter bounds
        bounds = glyph.getBounds();

        return glyph;
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, look for stem to allow head attachment.
     *
     * @return stem link, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        if (ShapeSet.StemHeads.contains(shape)) {
            // Not very optimized!
            List<Inter> systemStems = system.getSig().inters(StemInter.class);
            Collections.sort(systemStems, Inters.byAbscissa);

            Link link = lookupLink(systemStems);

            if (link != null) {
                if (doit) {
                    link.applyTo(this);
                }

                return Collections.singleton(link);
            }
        }

        return Collections.emptyList();
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Shrink a bit a bounding bounds when checking for note overlap.
     *
     * @param box the bounding bounds
     * @return the shrunk bounds
     */
    public static Rectangle2D shrink (Rectangle box)
    {
        double newWidth = constants.shrinkHoriRatio.getValue() * box.width;
        double newHeight = constants.shrinkVertRatio.getValue() * box.height;

        return new Rectangle2D.Double(
                box.getCenterX() - (newWidth / 2.0),
                box.getCenterY() - (newHeight / 2.0),
                newWidth,
                newHeight);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //--------------//
    // alterationOf //
    //--------------//
    /**
     * Report the pitch alteration that corresponds to the provided accidental.
     *
     * @param accidental the provided accidental
     * @return the pitch impact
     */
    private int alterationOf (AlterInter accidental)
    {
        switch (accidental.getShape()) {
        case SHARP:
            return 1;

        case DOUBLE_SHARP:
            return 2;

        case FLAT:
            return -1;

        case DOUBLE_FLAT:
            return -2;

        case NATURAL:
            return 0;

        default:
            logger.warn(
                    "Weird shape {} for accidental {}",
                    accidental.getShape(),
                    accidental.getId());

            return 0; // Should not happen
        }
    }

    //------------------//
    // fixDuplicateWith //
    //------------------//
    /**
     * Fix head duplication on two staves.
     * <p>
     * We have two note heads from different staves and with overlapping bound.
     * Vertical gap between the staves must be small and crowded, leading to head being "duplicated"
     * in both staves.
     * <p>
     * Assuming there is a linked stem, we could use sibling stem/head in a beam group if any.
     * Or we can simply use stem direction, assumed to point to the "true" containing staff.
     *
     * @param that the other inter
     */
    private void fixDuplicateWith (HeadInter that)
            throws DeletedInterException
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            StemInter thisStem = (StemInter) sig.getOppositeInter(this, rel);
            int thisDir = thisStem.computeDirection();
            Inter dupli = ((thisDir * (that.getStaff().getId() - this.getStaff().getId())) > 0)
                    ? this : that;

            logger.debug("Deleting duplicated {}", dupli);
            dupli.remove();
            throw new DeletedInterException(dupli);
        }

        //TODO: What if we have no stem? It's a WHOLE_NOTE or SMALL_WHOLE_NOTE
        // Perhaps check for a weak ledger, tangent to the note towards staff
    }

    //----------//
    // getAlter //
    //----------//
    /**
     * Report the actual alteration of this note, taking into account the accidental of
     * this note if any, the accidental of previous note with same step within the same
     * measure, a tie from previous measure and finally the current key signature.
     *
     * @param fifths fifths value for current key signature
     * @param useTie true to use tie for check
     * @return the actual alteration
     */
    private int getAlter (Integer fifths,
                          boolean useTie)
    {
        // Look for local accidental
        AlterInter accidental = getAccidental();

        if (accidental != null) {
            return alterationOf(accidental);
        }

        // Look for previous accidental with same note step in the same measure
        // Let's avoid the use of time slots (which would require RHYTHMS step to be done!)
        Measure measure = getChord().getMeasure();
        List<Inter> heads = new ArrayList<Inter>();

        for (HeadChordInter headChord : measure.getHeadChords()) {
            heads.addAll(headChord.getMembers());
        }

        boolean started = false;
        Collections.sort(heads, Inters.byReverseCenterAbscissa);

        for (Inter inter : heads) {
            HeadInter head = (HeadInter) inter;

            if (head == this) {
                started = true;
            } else if (started
                       && (head.getStep() == getStep())
                       && (head.getOctave() == getOctave())
                       && (head.getStaff() == getStaff())) {
                AlterInter accid = head.getAccidental();

                if (accid != null) {
                    return alterationOf(accid);
                }
            }
        }

        if (useTie) {
            // Look for tie from previous measure (same system or previous system)
            for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
                SlurInter slur = (SlurInter) sig.getOppositeInter(this, rel);

                if (slur.isTie() && (slur.getHead(HorizontalSide.RIGHT) == this)) {
                    // Is the starting head in same system?
                    HeadInter startHead = slur.getHead(HorizontalSide.LEFT);

                    if (startHead != null) {
                        // Use start head alter
                        return startHead.getAlter(fifths);
                    }

                    // Use slur extension to look into previous system
                    SlurInter prevSlur = slur.getExtension(HorizontalSide.LEFT);

                    if (prevSlur != null) {
                        startHead = prevSlur.getHead(HorizontalSide.LEFT);

                        if (startHead != null) {
                            // Use start head alter
                            return startHead.getAlter(fifths);
                        }
                    }

                    // TODO: Here we should look in previous sheet/page...
                }
            }
        }

        // Finally, use the current key signature
        if (fifths != null) {
            return KeyInter.getAlterFor(getStep(), fifths);
        }

        // Nothing found, so...
        return 0;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this Head instance and a stem nearby.
     * <p>
     * 1/ Use a lookup area on each horizontal side of the head to filter candidate stems.
     * 2/ Select the best connection among the compatible candidates.
     *
     * @param systemStems abscissa-ordered collection of stems in system
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemStems)
    {
        if (systemStems.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemStems.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int interline = scale.getInterline();
        final int maxHeadInDx = scale.toPixels(HeadStemRelation.getXInGapMaximum(manual));
        final int maxHeadOutDx = scale.toPixels(HeadStemRelation.getXOutGapMaximum(manual));
        final int maxYGap = scale.toPixels(HeadStemRelation.getYGapMaximum(manual));

        Link bestLink = null;
        double bestGrade = 0;

        for (Corner corner : Corner.values) {
            Point refPt = PointUtil.rounded(
                    getStemReferencePoint(corner.stemAnchor(), interline));
            int xMin = refPt.x - ((corner.hSide == RIGHT) ? maxHeadInDx : maxHeadOutDx);
            int yMin = refPt.y - ((corner.vSide == TOP) ? maxYGap : 0);
            Rectangle luBox = new Rectangle(xMin, yMin, maxHeadInDx + maxHeadOutDx, maxYGap);
            List<Inter> stems = Inters.intersectedInters(systemStems, GeoOrder.BY_ABSCISSA, luBox);
            int xDir = (corner.hSide == RIGHT) ? 1 : (-1);

            for (Inter inter : stems) {
                StemInter stem = (StemInter) inter;
                final Point2D start = stem.getTop();
                final Point2D stop = stem.getBottom();

                double crossX = LineUtil.xAtY(start, stop, refPt.getY());
                final double xGap = xDir * (crossX - refPt.getX());
                final double yGap;

                if (refPt.getY() < start.getY()) {
                    yGap = start.getY() - refPt.getY();
                } else if (refPt.getY() > stop.getY()) {
                    yGap = refPt.getY() - stop.getY();
                } else {
                    yGap = 0;
                }

                HeadStemRelation rel = new HeadStemRelation();
                rel.setInOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestLink == null) || (rel.getGrade() > bestGrade)) {
                        rel.setExtensionPoint(refPt); // Approximately
                        bestLink = new Link(stem, rel, true);
                        bestGrade = rel.getGrade();
                    }
                }
            }
        }

        return bestLink;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"dist"};

        private static final double[] WEIGHTS = new double[]{1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.5,
                "Horizontal shrink ratio to apply when checking note overlap");

        private final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Vertical shrink ratio to apply when checking note overlap");

        private final Constant.Ratio maxOverlapDxRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable abscissa overlap ratio between notes");

        private final Constant.Ratio maxOverlapAreaRatio = new Constant.Ratio(
                0.25,
                "Maximum acceptable box area overlap ratio between notes");
    }
}
