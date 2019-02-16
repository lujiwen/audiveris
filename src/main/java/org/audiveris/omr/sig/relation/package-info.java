/**
 * Package for all relations between interpretations used by SIG.
 */
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(value = Jaxb.PathAdapter.class, type = Path.class),
    @XmlJavaTypeAdapter(value = Jaxb.PointAdapter.class, type = Point.class),
    @XmlJavaTypeAdapter(value = Jaxb.Point2DAdapter.class, type = Point2D.class),
    @XmlJavaTypeAdapter(value = Jaxb.RectangleAdapter.class, type = Rectangle.class)
})
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.nio.file.Path;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
