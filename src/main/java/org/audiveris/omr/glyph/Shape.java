//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h a p e                                            //
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Enum {@code Shape} defines the comprehensive enumeration of glyph shapes.
 * <p>
 * The enumeration begins with physical shapes (which are the only ones usable for training) and
 * ends with additional (logical) shapes. The method {@link #isTrainable()} can be used to
 * disambiguate between physical and logical shapes.
 * <p>
 * <b>Nota</b>: All the physical shapes <b>MUST</b> have different characteristics for the training
 * to work correctly.
 * The same physical shape can lead to different logical shapes according to the context.
 * Two physical shapes are in this case (their name ends with "<i>_set</i>" to make this clear):
 * <ul>
 * <li>Physical DOT_set: only the context can disambiguate between:
 * <ul>
 * <li>an augmentation dot (first or second dot),
 * <li>a part of a repeat sign (upper or lower dot),
 * <li>a staccato sign,
 * <li>a part of fermata sign,
 * <li>a dot of an ending indication,
 * <li>a simple text dot.
 * </ul>
 * </li>
 * <li>Physical HW_REST_set: depending on the precise pitch position within the staff, it can mean
 * different logicals:
 * <ul>
 * <li>HALF_REST</li>
 * <li>WHOLE_REST</li>
 * </ul>
 * </ul>
 * As far as possible, a display symbol should be generated for every shape.
 * <p>
 * A shape may have a related "decorated" symbol. For example the BREVE_REST is similar to a black
 * rectangle which is used for training / recognition and the related symbol is used for drawing in
 * score view. However, in menu items, it is displayed as a black rectangle surrounded by a staff
 * line above and a staff line below.
 * The method {@link #getDecoratedSymbol()} returns the symbol to use in menu items.
 *
 * @author Hervé Bitteur
 */
public enum Shape
{
    /**
     * =================================================================================
     * Nota: Avoid changing the order of these physical shapes, otherwise the evaluators
     * won't detect this and you'll have to retrain them on your own.
     * =============================================================================================
     */

    //
    // Sets --------------------------------------------------------------------
    //
    DOT_set("Dot set"),
    HW_REST_set("Half & Whole Rest set"),

    //
    // Bars --------------------------------------------------------------------
    //
    DAL_SEGNO("D.S.: Repeat from the sign"),
    DA_CAPO("D.C.: Repeat from the beginning"),
    SEGNO("Sign"),
    CODA("Closing section"),
    BREATH_MARK("Breath Mark"),
    CAESURA("Caesura"),
    FERMATA_ARC("Fermata arc, without dot"),
    FERMATA_ARC_BELOW("Fermata arc below, without dot"),

    //
    // Clefs -------------------------------------------------------------------
    //
    G_CLEF("Treble Clef"),
    G_CLEF_SMALL("Small Treble Clef"),
    G_CLEF_8VA("Treble Clef Ottava Alta"),
    G_CLEF_8VB("Treble Clef Ottava Bassa"),
    C_CLEF("Ut Clef"),
    F_CLEF("Bass Clef"),
    F_CLEF_SMALL("Small Bass Clef"),
    F_CLEF_8VA("Bass Clef Ottava Alta"),
    F_CLEF_8VB("Bass Clef Ottava Bassa"),
    PERCUSSION_CLEF("Percussion Clef"),

    //
    // Accidentals -------------------------------------------------------------
    //
    FLAT("Minus one half step"),
    NATURAL("Natural value"),
    SHARP("Plus one half step"),
    DOUBLE_SHARP("Double Sharp"),
    DOUBLE_FLAT("Double Flat"),

    //
    // Time --------------------------------------------------------------------
    //
    TIME_ZERO("Time digit 0"),
    TIME_ONE("Time digit 1"),
    TIME_TWO("Time digit 2"),
    TIME_THREE("Time digit 3"),
    TIME_FOUR("Time digit 4"),
    TIME_FIVE("Time digit 5"),
    TIME_SIX("Time digit 6"),
    TIME_SEVEN("Time digit 7"),
    TIME_EIGHT("Time digit 8"),
    TIME_NINE("Time digit 9"),
    TIME_TWELVE("Time number 12"),
    TIME_SIXTEEN("Time number 16"),

    // Whole time sigs
    COMMON_TIME("Alpha = 4/4"),
    CUT_TIME("Semi-Alpha = 2/2"),
    // Predefined time combos
    TIME_FOUR_FOUR("Rational 4/4"),
    TIME_TWO_TWO("Rational 2/2"),
    TIME_TWO_FOUR("Rational 2/4"),
    TIME_THREE_FOUR("Rational 3/4"),
    TIME_FIVE_FOUR("Rational 5/4"),
    TIME_THREE_EIGHT("Rational 3/8"),
    TIME_SIX_EIGHT("Rational 6/8"),

    // Octave shifts
    OTTAVA_ALTA("8 va"),
    OTTAVA_BASSA("8 vb"),
    //
    // Rests -------------------------------------------------------------------
    //
    LONG_REST("Rest for 4 measures"),
    BREVE_REST("Rest for 2 measures"),
    QUARTER_REST("Rest for a 1/4"),
    EIGHTH_REST("Rest for a 1/8"),
    ONE_16TH_REST("Rest for a 1/16"),
    ONE_32ND_REST("Rest for a 1/32"),
    ONE_64TH_REST("Rest for a 1/64"),
    ONE_128TH_REST("Rest for a 1/128"),

    //
    // Flags -------------------------------------------------------------------
    //
    FLAG_1("Single flag down"),
    FLAG_1_UP("Single flag up"),
    FLAG_2("Double flag down"),
    FLAG_2_UP("Double flag up"),
    FLAG_3("Triple flag down"),
    FLAG_3_UP("Triple flag up"),
    FLAG_4("Quadruple flag down"),
    FLAG_4_UP("Quadruple flag up"),
    FLAG_5("Quintuple flag down"),
    FLAG_5_UP("Quintuple flag up"),

    //
    // Small Flags
    //
    SMALL_FLAG("Flag for grace note"),
    SMALL_FLAG_SLASH("Flag for slashed grace note"),

    //
    // StemLessHeads -----------------------------------------------------------
    //
    BREVE("Double Whole"),
    //
    // Articulation ------------------------------------------------------------
    //
    ACCENT,
    TENUTO,
    STACCATISSIMO,
    STRONG_ACCENT("Marcato"),
    ARPEGGIATO,

    //
    // Dynamics ----------------------------------------------------------------
    //
    //    DYNAMICS_CHAR_M("m character"),
    //    DYNAMICS_CHAR_R("r character"),
    //    DYNAMICS_CHAR_S("s character"),
    //    DYNAMICS_CHAR_Z("z character"),
    //    DYNAMICS_FFF("Fortississimo"),
    //    DYNAMICS_FZ("Forzando"),
    //    DYNAMICS_PPP("Pianississimo"),
    //    DYNAMICS_RF,
    //    DYNAMICS_RFZ("Rinforzando"),
    //    DYNAMICS_SFFZ,
    //    DYNAMICS_SFP("Subito fortepiano"),
    //    DYNAMICS_SFPP,
    DYNAMICS_P("Piano"),
    DYNAMICS_PP("Pianissimo"),
    DYNAMICS_MP("Mezzo piano"),
    DYNAMICS_F("Forte"),
    DYNAMICS_FF("Fortissimo"),
    DYNAMICS_MF("Mezzo forte"),
    DYNAMICS_FP("FortePiano"),
    DYNAMICS_SF("Subito forte"),
    DYNAMICS_SFZ("Sforzando"),

    //
    // Ornaments ---------------------------------------------------------------
    //
    TR("Trill"),
    TURN("Turn"),
    TURN_INVERTED("Inverted Turn"),
    TURN_UP("Turn Up"),
    TURN_SLASH("Turn with a Slash"),
    MORDENT("Mordent"),
    MORDENT_INVERTED("Mordent with a Slash"),

    //
    // Tuplets -----------------------------------------------------------------
    //
    TUPLET_THREE("3"),
    TUPLET_SIX("6"),
    PEDAL_MARK("Pedal down"),
    PEDAL_UP_MARK("Pedal downup"),

    //
    // Small digits ------------------------------------------------------------
    //
    DIGIT_0("Digit 0"),
    DIGIT_1("Digit 1"),
    DIGIT_2("Digit 2"),
    DIGIT_3("Digit 3"),
    DIGIT_4("Digit 4"),
    DIGIT_5("Digit 5"),

    //    DIGIT_6("Digit 6"),
    //    DIGIT_7("Digit 7"),
    //    DIGIT_8("Digit 8"),
    //    DIGIT_9("Digit 9"),
    //
    // Roman numerals ----------------------------------------------------------
    //
    ROMAN_I("Roman number 1"),
    ROMAN_II("Roman number 2"),
    ROMAN_III("Roman number 3"),
    ROMAN_IV("Roman number 4"),
    ROMAN_V("Roman number 5"),
    ROMAN_VI("Roman number 6"),
    ROMAN_VII("Roman number 7"),
    ROMAN_VIII("Roman number 8"),
    ROMAN_IX("Roman number 9"),
    ROMAN_X("Roman number 10"),
    ROMAN_XI("Roman number 11"),
    ROMAN_XII("Roman number 12"),

    //
    // Plucking ----------------------------------------------------------------
    //
    PLUCK_P("Plucking pouce/pulgar/thumb"),
    PLUCK_I("Plucking index/indicio/index"),
    PLUCK_M("Plucking majeur/medio/middle"),
    PLUCK_A("Plucking annulaire/anular/ring"),

    //
    // Miscellaneous -----------------------------------------------------------
    //
    CLUTTER("Pure clutter", Colors.SHAPE_UNKNOWN),
    /**
     * =================================================================================
     * End of physical shapes, beginning of logical shapes.
     * =============================================================================================
     */
    TEXT("Sequence of letters & spaces"),
    CHARACTER("Any letter"),

    //
    // Shapes from shape sets --------------------------------------------------
    //
    REPEAT_DOT("Repeat dot", DOT_set),
    AUGMENTATION_DOT("Augmentation Dot", DOT_set),
    FERMATA_DOT("Fermata Dot", DOT_set),
    STACCATO("Staccato dot", DOT_set),

    //
    WHOLE_REST("Rest for whole measure", HW_REST_set),
    HALF_REST("Rest for a 1/2", HW_REST_set),

    //
    // Noteheads ---------------------------------------------------------------
    //
    NOTEHEAD_BLACK("Filled node head for quarters and less"),
    NOTEHEAD_BLACK_SMALL("Small filled note head for grace or cue"),
    NOTEHEAD_VOID("Hollow node head for halves"),
    NOTEHEAD_VOID_SMALL("Small hollow note head for grace or cue"),

    //
    // StemLessHeads -----------------------------------------------------------
    //
    WHOLE_NOTE("Hollow node head for wholes"),
    WHOLE_NOTE_SMALL("Small hollow node head for grace or cue wholes"),

    //
    // Beams and slurs ---------------------------------------------------------
    //
    BEAM("Beam between two stems"),
    BEAM_SMALL("Small beam for cue notes"),
    BEAM_HOOK("Hook of a beam attached on one stem"),
    BEAM_HOOK_SMALL("Small hook of a beam for cue notes"),
    SLUR("Slur tying notes"),

    //
    // Key signatures ----------------------------------------------------------
    //
    KEY_FLAT_7("Seven Flats"),
    KEY_FLAT_6("Six Flats"),
    KEY_FLAT_5("Five Flats"),
    KEY_FLAT_4("Four Flats"),
    KEY_FLAT_3("Three Flats"),
    KEY_FLAT_2("Two Flats"),
    KEY_FLAT_1("One Flat"),
    KEY_SHARP_1("One Sharp"),
    KEY_SHARP_2("Two Sharps"),
    KEY_SHARP_3("Three Sharps"),
    KEY_SHARP_4("Four Sharps"),
    KEY_SHARP_5("Five Sharps"),
    KEY_SHARP_6("Six Sharps"),
    KEY_SHARP_7("Seven Sharps"),

    //
    // Bars --------------------------------------------------------------------
    //
    THIN_BARLINE("Thin bar line"),
    THIN_CONNECTOR("Connector between thin barlines", Colors.SCORE_FRAME),
    THICK_BARLINE("Thick bar line"),
    THICK_CONNECTOR("Connector between thick barlines", Colors.SCORE_FRAME),
    BRACKET_CONNECTOR("Connector between bracket items", Colors.SCORE_FRAME),
    DOUBLE_BARLINE("Double thin bar line"),
    FINAL_BARLINE("Thin / Thick bar line"),
    REVERSE_FINAL_BARLINE("Thick / Thin bar line"),
    LEFT_REPEAT_SIGN("Thick / Thin bar line + Repeat dots"),
    RIGHT_REPEAT_SIGN("Repeat dots + Thin / Thick bar line"),
    BACK_TO_BACK_REPEAT_SIGN("Repeat dots + Thin / Thick / Thin + REPEAT_DOTS"),
    ENDING("Alternate ending"),
    //
    // Wedges ------------------------------------------------------------------
    //
    CRESCENDO("Crescendo"),
    DIMINUENDO("Diminuendo"),
    //
    // Miscellaneous
    //
    BRACE("Brace"),
    BRACKET("Bracket"),
    REPEAT_DOT_PAIR("Pair of repeat dots"),
    NOISE("Too small stuff", Colors.SHAPE_UNKNOWN),
    LEDGER("Ledger"),
    ENDING_HORIZONTAL("Horizontal part of ending"),
    ENDING_VERTICAL("Vertical part of ending"),
    SEGMENT("Wedge or ending segment"),
    LYRICS("Lyrics", Colors.SCORE_LYRICS),

    //
    // Stems
    //
    STEM("Stem"),
    //
    // Ornaments ---------------------------------------------------------------
    //
    GRACE_NOTE_SLASH("Grace Note with a Slash"),
    GRACE_NOTE("Grace Note with no slash"),

    //
    // Full fermatas -----------------------------------------------------------
    //
    FERMATA("Fermata with dot"),
    FERMATA_BELOW("Fermata below with dot"),

    //
    // Other stuff -------------------------------------------------------------
    //
    FORWARD("To indicate a forward"),
    NON_DRAGGABLE("Non draggable shape"),
    GLYPH_PART("Part of a larger glyph"),
    CUSTOM_TIME("Time signature defined by user"),
    NO_LEGAL_TIME("No Legal Time Shape");

    //
    // =============================================================================================
    // This is the end of shape enumeration
    // =============================================================================================
    //
    private static final Logger logger = LoggerFactory.getLogger(Shape.class);

    /** Last physical shape */
    public static final Shape LAST_PHYSICAL_SHAPE = CLUTTER;

    /** A comparator based on shape name */
    public static Comparator<Shape> alphaComparator = new Comparator<Shape>()
    {
        @Override
        public int compare (Shape o1,
                            Shape o2)
        {
            return o1.name().compareTo(o2.name());
        }
    };

    //
    /** Explanation of the glyph shape */
    private final String description;

    /** Potential related symbol */
    private ShapeSymbol symbol;

    /** Potential related decorated symbol for menus */
    private ShapeSymbol decoratedSymbol;

    /** Remember the fact that this shape has no related symbol */
    private boolean hasNoSymbol;

    /** Remember the fact that this shape has no related decorated symbol */
    private boolean hasNoDecoratedSymbol;

    /** Potential related physical shape */
    private Shape physicalShape;

    /** Related color */
    private Color color;

    /** Related color constant */
    private Constant.Color constantColor;

    //--------------------------------------------------------------------------
    //
    //-------//
    // Shape //
    //-------//
    Shape ()
    {
        this("", null, null);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description)
    {
        this(description, null, null);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Color color)
    {
        this(description, null, color);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Shape physicalShape)
    {
        this(description, physicalShape, null);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Shape physicalShape,
           Color color)
    {
        this.description = description;
        this.physicalShape = physicalShape;
        this.color = color;

        // Create the underlying constant
        constantColor = new Constant.Color(
                getClass().getName(), // Unit
                name() + ".color", // Name
                Constant.Color.encodeColor((color != null) ? color : Color.BLACK),
                "Color for shape " + name());
    }

    //-------------//
    // isWholeRest //
    //-------------//
    /**
     * Check whether the shape is a whole (or multi) rest, for which no duration can be
     * specified.
     *
     * @return true if whole / multi rest
     */
    public boolean isWholeRest ()
    {
        return (this == WHOLE_REST) || (this == BREVE_REST) || (this == LONG_REST);
    }

    //--------//
    // isHead //
    //--------//
    /**
     * Check whether the shape is a head.
     *
     * @return true if head
     */
    public boolean isHead ()
    {
        return ShapeSet.Heads.contains(this);
    }

    //--------//
    // isRest //
    //--------//
    /**
     * Check whether the shape is a rest.
     *
     * @return true if rest
     */
    public boolean isRest ()
    {
        return ShapeSet.Rests.contains(this);
    }

    //--------------//
    // isPersistent //
    //--------------//
    /**
     * Report whether the impact of this shape persists across system
     * (actually measure) borders (clefs, time signatures, key signatures).
     * Based on just the shape, we cannot tell whether an accidental is part of
     * a key signature or not, so we take a conservative approach.
     *
     * @return true if persistent, false otherwise
     */
    public boolean isPersistent ()
    {
        return ShapeSet.Clefs.contains(this) || ShapeSet.Times.contains(this)
               || ShapeSet.Accidentals.contains(this);
    }

    //--------//
    // isText //
    //--------//
    /**
     * Check whether the shape is a text (or a simple character).
     *
     * @return true if text or character
     */
    public boolean isText ()
    {
        return (this == TEXT) || (this == CHARACTER);
    }

    //--------------//
    // isSharpBased //
    //--------------//
    /**
     * Check whether the shape is a sharp or a key-sig sequence of sharps.
     *
     * @return true if sharp or sharp key sig
     */
    public boolean isSharpBased ()
    {
        return (this == SHARP) || ShapeSet.SharpKeys.contains(this);
    }

    //-------------//
    // isFlatBased //
    //-------------//
    /**
     * Check whether the shape is a flat or a key-sig sequence of flats.
     *
     * @return true if flat or flat key sig
     */
    public boolean isFlatBased ()
    {
        return (this == FLAT) || ShapeSet.FlatKeys.contains(this);
    }

    //---------//
    // isSmall //
    //---------//
    /**
     * Check whether the shape is a small note, meant for cue or grace.
     *
     * @return true if small (black/void/whole)
     */
    public boolean isSmall ()
    {
        return ShapeSet.SmallNotes.contains(this);
    }

    //-------------//
    // isTrainable //
    //-------------//
    /**
     * Report whether this shape can be used to train an classifier.
     *
     * @return true if trainable, false otherwise
     */
    public boolean isTrainable ()
    {
        return ordinal() <= LAST_PHYSICAL_SHAPE.ordinal();
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a user-friendly description of this shape.
     *
     * @return the shape description
     */
    public String getDescription ()
    {
        if (description == null) {
            return toString(); // Could be improved
        } else {
            return description;
        }
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color assigned to the shape, if any.
     *
     * @return the related color, or null
     */
    public Color getColor ()
    {
        return color;
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Assign a color for this shape.
     *
     * @param color the display color
     */
    public void setColor (Color color)
    {
        this.color = color;
    }

    //------------------//
    // setConstantColor //
    //------------------//
    /**
     * Define a specific color for the shape.
     *
     * @param color the specified color
     */
    public void setConstantColor (Color color)
    {
        constantColor.setValue(color);
        setColor(color);
    }

    //------------------//
    // createShapeColor //
    //------------------//
    void createShapeColor (Color color)
    {
        // Assign the shape display color
        if (!constantColor.isSourceValue()) {
            setColor(constantColor.getValue()); // Use the shape specific color
        } else if (this.color == null) {
            setColor(color); // Use the provided (range) default color
        }
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Report the symbol related to the shape, if any.
     *
     * @return the related symbol, or null
     */
    public ShapeSymbol getSymbol ()
    {
        if (hasNoSymbol) {
            return null;
        }

        if (symbol == null) {
            symbol = Symbols.getSymbol(this);

            if (symbol == null) {
                hasNoSymbol = true;
            }
        }

        return symbol;
    }

    //-----------//
    // setSymbol //
    //-----------//
    /**
     * Assign a symbol to this shape.
     *
     * @param symbol the assigned symbol, which may be null
     */
    public void setSymbol (ShapeSymbol symbol)
    {
        this.symbol = symbol;
    }

    //--------------------//
    // getDecoratedSymbol //
    //--------------------//
    /**
     * Report the symbol to use for menu items.
     *
     * @return the shape symbol, with decorations if any
     */
    public ShapeSymbol getDecoratedSymbol ()
    {
        // Avoid a new search, just use the undecorated symbol instead
        if (hasNoDecoratedSymbol) {
            return getSymbol();
        }

        // Try to build / load a decorated symbol
        if (decoratedSymbol == null) {
            setDecoratedSymbol(Symbols.getSymbol(this, true));

            if (decoratedSymbol == null) {
                hasNoDecoratedSymbol = true;

                return getSymbol();
            }
        }

        // Simply return the cached decorated symbol
        return decoratedSymbol;
    }

    //--------------------//
    // setDecoratedSymbol //
    //--------------------//
    /**
     * Assign a decorated symbol to this shape.
     *
     * @param decoratedSymbol the assigned decorated symbol, which may be null
     */
    public void setDecoratedSymbol (ShapeSymbol decoratedSymbol)
    {
        this.decoratedSymbol = decoratedSymbol;
    }

    //------------------//
    // getPhysicalShape //
    //------------------//
    /**
     * Report the shape to use for training or precise drawing.
     *
     * @return the related physical shape, if different
     */
    public Shape getPhysicalShape ()
    {
        if (physicalShape != null) {
            return physicalShape;
        } else {
            return this;
        }
    }

    //-------------//
    // isDraggable //
    //-------------//
    /**
     * Report whether this shape can be dragged (in a DnD gesture).
     *
     * @return true if shape can be dragged
     */
    public boolean isDraggable ()
    {
        return getPhysicalShape().getSymbol() != null;
    }

    //-----------------//
    // dumpShapeColors //
    //-----------------//
    /**
     * Dump the color of every shape.
     */
    public static void dumpShapeColors ()
    {
        List<String> names = new ArrayList<String>();

        for (Shape shape : Shape.values()) {
            names.add(shape + " " + Constant.Color.encodeColor(shape.getColor()));
        }

        Collections.sort(names);

        for (String str : names) {
            System.out.println(str);
        }
    }
}
