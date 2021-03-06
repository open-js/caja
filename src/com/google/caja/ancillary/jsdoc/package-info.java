/**
Extracts documentation from JavaScript source files.

<h3>Documentation Syntax</h3>
Documentation is extracted using JavaDoc like annotations inside
<code>/** &hellip; *<!---->/</code> comments.

<p>The annotations are defined in
{@link com.google.caja.ancillary.jsdoc.AnnotationHandlers}.  Below is the
default set, but additional handlers can be added.
<table summary="annotations">
<tr><th>Annotation<th>Signature<th>Description</tr>
<tr>
  <td><code>&#64;author</code></td>
  <td>email | name '&lt;' email '&gt;' | email '(' name ')'</td>
  <td>The name of the author of the API element or file.</td>
<tr>
  <td><code>{&#64;code &hellip;}</code></td>
  <td>source</td>
  <td>Embedded and syntax highlighted source code</td>
<tr>
  <td colspan=3>TODO: finish
</table>


<h3>Usage</h3>
<p>To generate JSON, run
<pre>  java -jar jsdoc.jar file1.js file2.js file3.js foo/package.html</pre>
and JSDoc will write a JSON file describing all the API elements and files
extracted.

<p>This file can be plugged into the AJAX doc viewer, or you can generate a
static HTML tree by doing
<pre>  java -jar jsdoc.jar -o out_dir file1.js foo/package.html</pre>
which will output both a <tt>jsdoc.json</tt>, and an html file tree under
<tt>out_dir</tt> file.
<pre class="prettyprint">
out_dir
  index.html
  jsdoc.json
</pre>


<h3>Types in <code>&#64;param</code>, <code>&#64;return</code>, etc.</h3>

<p>In keeping with existing static-analysis based jsdoc tools, JSDoc
supports most of the java annotations plus a few that attempt to bolt
an EcmaScript 4 style type system onto EcmaScript 3,

<p>JavaDoc has <code>&#64;param</code> and <code>&#64;return</code> annotations
and JSDoc supports the java syntax but adds an optional syntax where the
first element is a type in curly brackets.<pre class="prettyprint">
/&#42;*
 &#42; &#64;param {<u>string</u>} html the HTML to translate
 &#42; &#64;return {<u>string</u>} the plain text equivalent of the input HTML
 &#42;/
function htmlToText(html) { &hellip; }
</pre>

<p>The new <code>&#64;type</code> annotation describes the type of the
declaration.<pre class="prettyprint">
/&#42;*
 &#42; The ratio of a circle's circumference to its diameter.
 &#42; &#64;type {<u>number</u>}
 &#42;/
var PI = 3.141592654;
</pre>

<p>JSDoc does not do type checking, and so is agnostic to the type
syntax and as to whether, for function declarations the
<code>&#64;type</code> specifies the return type of the function, or the
type of the function as a whole.  Since existing type checking efforts
have taking conflicting sides on this, it's best to use
<code>&#64;param</code> and <code>&#64;return</code> and <code>&#64;this</code>
for describing functions.

<p>JSDoc will warn you if identifiers that appear in types are not
valid symbols in the current scope.  For example, in
<pre class="prettyprint">
/&#42;*
 &#42; &#64;type {<u>Array</u>.&lt;<u>foo.Bar</u>&gt;}
 &#42;/
</pre>
JSDoc will look for the symbols <code>Array</code> and
<code>foo.Bar</code> in the scope in which the comment appears.  If
those values are undefined, or evaluating them raises an exception,
then JSDoc issues a warning.

<h3>Why Dynamic Analysis?</h3>
<p>Many languages have a clear way of separating a module's public API from its
private implementation details.  In JavaScript, that's done by using lexical
scopes to bundle together elements that are connected, as in:
<pre class="prettyprint">
var myModule = (function () {  // function creates a hidden scope
  // Define private variables
  var serialNumber = 0;

  // Define public stuff
  function getSerialNumber() { return serialNumber++; }

  // Export the public API
  return {
    getSerialNumber: getSerialNumber
  };
})();
</pre>
<p>There are many variations on this theme, but they all do a good job of
making life hard for programs that try to figure out a JavaScript program's
API just by looking at its source code, and so they either miss large parts of
the API, or force their users to write JavaScript in a style that doesn't take
advantage of JavaScript's strengths such as closures and scoping for information
hiding.</p>
<p>Specifically, static analyzers tend to have trouble with
<ol>
  <li>JavaScript style information hiding</li>
  <li>Monkey patching</li>
  <li>Class definition that involves mixins</li>
</ol>
<p>JSDoc is more accurate because it skips static analysis and instead just runs
the program and looks at what it produces.  By looking at the program after it's
run, JSDoc sees exactly what the programmer who wants to use the module would.

<h3>Tests as Documentation</h3>
<p>JSDoc adds a new annotation <code>&#64;updoc</code> to allow tests
to be embedded in code.
<p>Frequently, java and python code uses private members as a way to
hide implementation details.  Testing frameworks such as
PyUnit and JUnitX each work around the inaccessibility of private members so
that those API elements can be tested.
<p>But in JavaScript, there is no <code>private</code> API, so careful
developers hide implementation details by using lexical scopes which
are impenetrable to unit testing (modulo {@code eval} extensions in
Firefox &lt;= 3.0).
<p>JSDoc finds <code>&#64;updoc</code> tests in source code, runs
them, and includes test results in the documentation.
<p>See {@link com.google.caja.ancillary.jsdoc.Updoc} for examples and usage.


<h3>Design</h3>
<h4>Goals</h4>
<p>Extract JavaDoc style documentation from JavaScript source code
without restricting the way that JavaScript developers structure their
code.
<p>The documentation for a module should reflect the API elements added or
modified by that module.  It is a non-goal to document the private
implementation details of a module.
<p>Produce a view of the API that can be used with other tools such as IDE
auto-completion &amp; name suggestion by a module.
<p>It is non a goal to document any private or protected API or hidden
implementation details.</p>


<h4>Overview</h4>
<pre>
  --------+
 /        |        +-----------+        -----+      +-----------+        -----+
|   --------+      |           |       /     |      |           |       /     |
|  /        |  =>  | Extractor |  =>  | JSON |  =>  | Formatter |  =>  | HTML |
+-| JS File |      |           |      |      |      |           |      |      |
  |         |      +-----------+      +------+      +-----------+      +------+
  +---------+
</pre>

<p>JSDoc takes in a group of JavaScript files.  An <b>extractor</b> rewrites
those files to attach comments to declarations and definitions, and then uses
<b>Rhino</b> to execute the rewritten JavaScript.

<p>The executed JavaScript is run in the context of <code>jsdoc.js</code>, and
the <code>extractDocs</code> function builds up a JSON structure representing
the elements added to the API.  It does this by comparing the API present
before the module was executed with that after, so the extraction algorithm
looks like:<ol>
<li>original_api := snapshot(global_namespace)
<li>execute_rewritten_module()
<li>modified_api := snapshot(global_namespace)
<li>module_api := modified_api - original_api
<li>return module_api
</ol>

<p>Snapshotting the API involves recursively walking everything
reachable from the global object.  As we walk each object, we attach a
name to the object, so if we reach an object via the <tt>bar</tt>
property of an object that we reached via the <tt>foo</tt> property of
the global object, then we know the object can be referenced as
<tt>foo.bar</tt> and so we assign it the name <tt>foo.bar</tt>.  We
walk the graph breadth-first so as to assign the shortest possible
name to each API element.  If we reach an object by more than one
path, we mark the second and subsequent names as "aliases" and don't
recurse to their properties.  The snapshotting process looks like this<ol>
<li>Walk the object graph assigning names to nodes.  Be sure not to
miss intrinsics like <tt>Array</tt> that are <tt>in</tt> the global
object, but that are missed by <tt>for (k in global)</tt>.
<li>Resolve "promises".  In <pre>/&#42;* &#64;see foo &#42;/</pre> the documentation
depends on the name by which <code>foo</code> is reachable from the global
scope, not the local scope.  Since names are derived <b>after</b> execution,
the code that attaches delays linking documentation by putting the result in
an anonymous function:
<pre class="prettyprint"
>jsdoc___.document(..., { '&#64;see': function () { return nameOf(foo); } })</pre>
<li>Build a documentation tree like
<pre class="prettyprint">
{  // Documentation for the global scope
  "foo": {  // A member of the global scope
    "&#64;see": "myPackage.bar"  // Corresponds to a doc annotation
  }
}</pre>
<li>Remove names and other book-keeping properties added by the walk.
</ol>

<p>Diffing the APIs involves comparing two JSON trees.  Primitive values are
considered different if they differ by <code>!==</code>.  Objects and Arrays
are considered different if they have a different set of property names or if
their properties' values are recursively different.
Identical internal nodes (Arrays and objects) are removed.
This means that there will be no entry for <code>Array</code> unless some change
was made to its API but if a module defines <code>Array.addAll</code> then
there will be an <code>Array</code> entry in the resulting JSON.

<table summary="similarities to JavaDoc and pydoc">
<caption>Similarities to existing tools</caption>
<tr><th><th><tt>JavaDoc</tt><th><tt>Pydoc</tt><th><tt>JSDoc</tt><th>Reason</tr>
<tr>
  <th>Doc Strings</th>
  <td>In <code>/&#42;* &hellip; &#42;<!---->/</code> comments</td>
  <td>In the first string of the body</td>
  <td>As JavaDoc</td>
  <td>JS's comments are the same as java's (modulo unicode escapes)</td>
<tr>
  <th>Structure</th>
  <td>Comments contain <code>&#64;foo</code></td>
  <td>None
  <td>As JavaDoc with different annotations</td>
  <td>Most JavaDoc annotations plus a few JS specific ones.
<tr>
  <th>Extraction Style</th>
  <td>Static analysis</td>
  <td>Code evaluation</td>
  <td>As Pydoc but with a rewriting stage to turn comments into
  doc-strings.</td>
  <td>JS has first-class constructors and methods, so static-analysis
  won't work.</td>
</table>

<h4>Annotation Extraction</h4>
<p>Since JavaScript is a dynamic language, it's hard to tell statically from
a declaration site which annotations are appropriate in any comment.
E.g.<pre class="prettyprint">
/&#42;*
 &#42; &#64;param {number} x the x-coordinate
 &#42;/
var setX;
</pre>
looks like it defines a function, but we can only check that it is and it has
a formal parameter named "x", and introduce blank doc for any missing parameters
during execution.
<p>This example raises another issue.  How do we attach documentation to
something that doesn't exist yet?  We have options
<ol>
<li>Identify all sites that assign to x and attach the documentation to all
   values assigned to x.
<li>Document the value in x when x leaves scope
</ol>
<p>The first would lead us to attach documentation to any number of
objects and doesn't deal well with a variable being used as a
temporary until the code converges on the real value.  The second is
more in keeping with the principle that each documentation comment
corresponds to one API element, but deals poorly with a variable that
is multiply declared by someone attempting block scoping in
JavaScript.  We choose <b>2</b> since we can fix the block scoping
defect by computing apparent scopes instead of block level scopes if
necessary.
<p>We implement this "value on leaving scope" rewriting quite simply
<table summary="rewrite to attach documentation on scope exit">
<tr>
<td style="border:1px dotted black"><pre class="prettyprint"
>/&#42;* &#64;param {number} x &#42;/
var setX;
...
setX = function (x) { return -x; };</pre></td>
<td>&rarr;</td>
<td style="border:1px dotted black"><pre class="prettyprint"
>try {
  var setX;
  ...
  setX = function (x) { return -x; };
} finally {
  jsdoc___.document(  // Defined in jsdoc.js.  Attaches documentation to a value
      setX,           // The API element being documented.
      {               // A record containing the annotations from the comment.
        param: [
            function (apiElementName, apiElement) {  // The promise envelope.
              return (checkParam('x', apiElement),   // A runtime sanity check.
                      {                              // Decomposed documentation
                        name: 'x',
                        type: 'number'
                      });
            }
        ]
      });
}</pre></td>
</table>
This rewriting illustrates two concepts: <b>promises</b> and <b>checks</b>.
The <b>promise</b> is the function envelope which allows delay of execution of
documentation until the entire name graph has been computed.  We do this
because, by waiting until we know the name of an API element, we can issue a
better error message should <code>setX</code> not actually have a formal
parameter <code>x</code>.  And a <b>check</b> is any logic that might issue
an error if an annotation is not applicable in context or otherwise malformed.

<p>There is one case where dynamic analysis doesn't give us all the information
we need.  For the simple class definition
<pre class="prettyprint">
function Point(x, y) {
  this.x = x;
  this.y = y;
}
Point.prototype.getTheta = function () {
  return Math.atan2(this.y, this.x);
};
</pre>
the <code>Point</code> class has two members, <code>x</code> and
<code>y</code> but walking the API doesn't find this information since
it is quite likely that loading a module does not cause an instance
to be created for every class defined.  There are a few approaches we
can take to try and fix this:<ol>
<li>statically determine the set of members of {@code this} referenced in the
constructor

<li>do the same at runtime by extracting the list of members of this by
inspecting <code>myClass.prototype[methodName]</code>.
</ol>
We attach to function documentation, the list of members of <code>this</code>
that are directly referenced.  The current documentation formatter only uses
the members of functions which it determines are constructors, but it can be
changed to look at all the methods attached to a class's prototype.


<h4>JSON format</h4>
TODO

<h4>Rewriting Rules</h4>
TODO
*/
package com.google.caja.ancillary.jsdoc;
