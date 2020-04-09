/**
 * The MIT License
 * Copyright © 2017 DTL
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nl.dtls.fairdatapoint.util;

import nl.dtls.fairdatapoint.entity.exception.ValidationException;
import nl.dtls.fairmetadata4j.util.ValueFactoryHelper;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static nl.dtls.fairdatapoint.util.ResourceReader.getResource;
import static nl.dtls.fairmetadata4j.accessor.MetadataSetter.setRdfTypes;
import static nl.dtls.fairmetadata4j.util.RDFUtil.getSubjectBy;
import static nl.dtls.fairmetadata4j.util.ValueFactoryHelper.i;
import static nl.dtls.fairmetadata4j.util.ValueFactoryHelper.s;

public class RdfUtil {

    public static Model changeBaseUri(Model oldModel, String newBaseUri, List<String> rdfTypes) {
        // - get baseUri
        Resource oldBaseUri = rdfTypes
                .stream()
                .map(rdfType -> getSubjectBy(oldModel, RDF.TYPE, i(rdfType)))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Validation failed (no rdf:type was provided"));
        // - sanitize statements
        List<Statement> sanitizedStatements =
                new ArrayList<>(oldModel.filter(oldBaseUri, null, null))
                        .stream()
                        .map(oldStatement -> s(i(newBaseUri), oldStatement.getPredicate(), oldStatement.getObject()))
                        .collect(Collectors.toList());
        Model model = new LinkedHashModel();
        model.addAll(sanitizedStatements);
        setRdfTypes(model, i(newBaseUri), rdfTypes.stream().map(ValueFactoryHelper::i).collect(Collectors.toList()));
        return model;
    }

    public static Model readFile(String name, String baseUri) {
        return readFile(name, baseUri, RDFFormat.TURTLE);
    }

    public static Model readFile(String name, String baseUri, RDFFormat format) {
        try (InputStream inputStream = getResource(name).getInputStream()) {
            return Rio.parse(inputStream, baseUri, format);
        } catch (IOException e) {
            throw new ValidationException("Unable to read RDF (IO exception)");
        } catch (RDFParseException e) {
            throw new ValidationException("Unable to read RDF (parse exception)");
        } catch (RDFHandlerException e) {
            throw new ValidationException("Unable to read RDF (handler exception)");
        }
    }

    public static Model read(String content, String baseUri) {
        return read(content, baseUri, RDFFormat.TURTLE);
    }

    public static Model read(String content, String baseUri, RDFFormat format) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            return Rio.parse(inputStream, baseUri, format);
        } catch (IOException e) {
            throw new ValidationException("Unable to read RDF (IO exception)");
        } catch (RDFParseException e) {
            throw new ValidationException("Unable to read RDF (parse exception)");
        } catch (RDFHandlerException e) {
            throw new ValidationException("Unable to read RDF (handler exception)");
        }
    }

    public static String write(Model model) {
        return write(model, RDFFormat.TURTLE);
    }

    public static String write(Model model, RDFFormat format) {
        try (StringWriter out = new StringWriter()) {
            Rio.write(model, out, format, getWriterConfig());
            return out.toString();
        } catch (IOException e) {
            throw new ValidationException("Unable to write RDF (IO exception)");
        }
    }

    public static WriterConfig getWriterConfig() {
        WriterConfig config = new WriterConfig();
        config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        return config;
    }

}
