# gerber-lib
This is a library to handle [Gerber](https://www.ucamco.com/en/gerber) files. It follows revision 2020-09. Currently only reading is implemented.

# Reading Gerber files 
The read side consists of the following parts:
* GerberParser: A PEG parser parses the input and calls a `GerberParseEventHandler`
* GerberReadAdapter: Converts the parse events into higher level events according to the standard. Calls a `GerberReadEventHandler`
* GerberReadGeometricPrimitiveAdapter: Based on the gerber events, determines the geometric primitives to be drawn and calls a `GerberReadGeometricPrimitiveEventHandler`
* GerberRasterizer: Draws geometric primitives to a canvas, based on the events of the `GerberReadGeometricPrimitiveAdapter`