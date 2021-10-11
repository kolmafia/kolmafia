<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>

<xsl:template match="/">
  <html><head>
     <title><xsl:value-of select="botdata/name" /> (#<xsl:value-of select="botdata/playerid" />)</title>
  </head><body bgcolor="#F9F9FF">
  <blockquote>

	<p>In order to request a buff, send a k-mail (also known as "green message") to <xsl:value-of select="botdata/name" /> (#<xsl:value-of select="botdata/playerid" />).  The following price list indicates what should be attached to the message in order to receive the buff that you want.</p>

	<h2>Buffs Offered Once Per Day</h2>

	<ul>
	<xsl:for-each select="botdata/free-list/buffdata">
	<xsl:sort select="price" data-type="number" />

	<li>
		<strong><xsl:value-of select="price" /></strong>
		meat for
		<strong><xsl:value-of select="turns" /></strong>
		turns of
		<strong><xsl:value-of select="name" /></strong>
	</li>

	</xsl:for-each>
	</ul>

	<xsl:for-each select="botdata/normal-list/skillset">
	<xsl:sort select="name" />

	<h2><xsl:value-of select="name" /></h2>

	<blockquote>
	<xsl:for-each select="buffset">
	<p><u><xsl:value-of select="name" /></u></p>

	<ul>
	<xsl:for-each select="buffdata">
	<xsl:sort select="turns" data-type="number" />
	<xsl:sort select="price" data-type="number" />

	<li>
		<strong><xsl:value-of select="price" /></strong>
		meat for
		<strong><xsl:value-of select="turns" /></strong>
		turns
	</li>

	</xsl:for-each>
	</ul>

	</xsl:for-each>
	</blockquote>

	</xsl:for-each>

  </blockquote>
  </body></html>

</xsl:template>


</xsl:stylesheet>
