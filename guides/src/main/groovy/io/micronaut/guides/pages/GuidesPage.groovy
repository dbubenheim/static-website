package io.micronaut.guides.pages

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import io.micronaut.MultiLanguageGuide
import io.micronaut.Navigation
import io.micronaut.ProgrammingLanguage
import io.micronaut.ReadFileUtils
import io.micronaut.SingleLanguageGuide
import io.micronaut.guides.model.Category
import io.micronaut.Guide
import io.micronaut.guides.model.Tag
import io.micronaut.TextMenuItem
import io.micronaut.pages.Page

import java.text.SimpleDateFormat

@CompileStatic
class GuidesPage extends Page implements ReadFileUtils {

    public static final Integer NUMBER_OF_LATEST_GUIDES = 5
    private static final Integer MARGIN_TOP = 50

    String bodyClass = 'guides'
    List<Guide> guides
    Set<Tag> tags
    Tag tag
    Category category

    GuidesPage(List<Guide> guides, Set<Tag> tags) {
        this.guides = guides
        this.tags = tags
    }

    GuidesPage(List<Guide> guides, Set<Tag> tags, Tag tag) {
        this(guides, tags)
        this.tag = tag
    }

    GuidesPage(List<Guide> guides, Set<Tag> tags, Category category) {
        this(guides, tags)
        this.category = category
    }

    String getSlug() {
        if ( tag ) {
            return "${tag.slug.toLowerCase()}.html"
        }
        if ( category ) {
            return "${category.slug.toLowerCase()}.html"
        }
        'index.html'
    }

    @Override
    boolean doNotIndex() {
        if ( tag || category ) {
            return true
        }
        false
    }

    @Override
    String getHtmlHeadTitle() {
        'Guides | Micronaut Framework'
    }

    @CompileDynamic
    String renderGuide(Guide guide, String query = null) {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        html.li {
            if ( guide instanceof SingleLanguageGuide) {
                a class: 'guide', href: "https://guides.micronaut.io/${guide.name}/guide/index.html", guide.title
                guide.tags.each { String tag ->
                    span(style: 'display: none', class: 'tag', tag)
                }
            } else if (guide instanceof MultiLanguageGuide) {
                MultiLanguageGuide multiLanguageGuide = ((MultiLanguageGuide) guide)
                div(class: 'multiguide') {
                    span(class: 'title', guide.title)
                    for (ProgrammingLanguage lang :  multiLanguageGuide.githubSlugs.keySet())  {
                        Set<String> tagList = multiLanguageGuide.programmingLanguageTags[lang] as Set<String>
                        tagList << lang.toString().toLowerCase()

                        if (query == null || titlesMatchesQuery(guide.title, query) || tagsMatchQuery(tagList as List<String>, query)) {
                            div(class: 'align-left') {
                                a(class: 'lang', href: "https://guides.micronaut.io/${multiLanguageGuide.githubSlugs[lang].replaceAll('micronaut-guides/', '')}/guide/index.html") {
                                    mkp.yield(lang.name())
                                }
                                tagList.each { String tag ->
                                    span(style: 'display: none', class: 'tag', tag)
                                }
                            }
                        }
                    }
                }
            }

        }
        writer.toString()
    }

    boolean titlesMatchesQuery(String title, String query) {
        title.indexOf(query) != -1
    }
    boolean tagsMatchQuery(List<String> tags, String query) {
        tags.any { it.indexOf(query) != -1 }
    }


    @CompileDynamic
    @Override
    String getTitle() {
        if ( tag || category ) {
            StringWriter writer = new StringWriter()
            MarkupBuilder html = new MarkupBuilder(writer)
            html.div {
                a href: "${guidesUrl()}/index.html", 'Guides'
                if (tag) {
                    mkp.yieldUnescaped " &rarr; #${tag.title}"
                } else if (category) {
                    mkp.yieldUnescaped " &rarr; ${category.name}"
                }
            }
            return writer.toString()
        }
        'Guides'
    }

    @Override
    List<String> getJavascriptFiles() {
        List<String> jsFiles = super.getJavascriptFiles()
        jsFiles << ("${guidesUrl()}/javascripts/${timestamp ? (timestamp + '.') : ''}search.js" as String)
        jsFiles
    }

    @Override
    List<String> getCssFiles() {
        ["${guidesUrl()}/stylesheets/${timestamp ? (timestamp + '.') : ''}screen.css" as String]
    }

    @Override
    String getImageAssetPreffix() {
        "${guidesUrl()}/images/"
    }

    @Override
    TextMenuItem menuItem() {
        if ( tag ||category ){
            return null
        }
        Navigation.guidesMenuItem(guidesUrl())
    }

    @CompileDynamic
    String guideGroupByCategory(Category category, List<Guide> guides, boolean linkToCategory = true, String cssStyle = '') {
        List<Guide> categoryGuides = guides.findAll { it.category == category.name }
        if ( !categoryGuides ) {
            return ''
        }
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)

        html.div(class: "guidegroup", style: cssStyle) {
            div(class: "guidegroupheader") {
                img src: "${getImageAssetPreffix()}${category.image}" as String, alt: category.name
                if ( linkToCategory )  {
                    a(href: "${guidesUrl()}/categories/${category.slug}.html") {
                        h2 category.name
                    }
                } else {
                    h2 category.name
                }
            }
            ul {
                categoryGuides.each { mkp.yieldUnescaped renderGuide(it) }
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String guideGroupByTag(Tag tag, List<Guide> guides) {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)

        html.div(class: "guidegroup") {
            div(class: "guidegroupheader") {
                img src: "${getImageAssetPreffix()}documentation.svg" as String, alt: 'Guides'
                h2 "Guides filtered by #${tag.title}"
            }
            ul {
                List<Guide> tagGuides = guides.findAll { Guide guide -> guide.tags.contains(tag.title) }
                tagGuides.each { mkp.yieldUnescaped renderGuide(it, tag.title) }
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String guideSuggestion() {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        html.div(class: 'guidesuggestion') {
            h3 class: 'columnheader', 'Which topic would you like us to cover?'
            String formHtml = readFileContent('guidesuggestionform.html')
            if ( formHtml ) {
                mkp.yieldUnescaped formHtml
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String searchBox(String id) {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        if ( !(tag || category) ) {
            html.div(class: 'searchbox') {
                div(class: 'search', style: 'margin-bottom: 0px !important;') {
                    input(type: 'text', id: id, placeholder: 'SEARCH')
                }
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String sponsoredBy() {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        html.div(class: 'sponsoredby', style: 'margin-top: 50px;') {
            h4 'Sponsored by'
            a(href: 'https://objectcomputing.com/products/micronaut/') {
                img src: "${getImageAssetPreffix()}oci-home-to-micronaut.svg", alt: 'Object Computing', width: '250px'
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String latestGuides() {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        html.div(class: 'latestguides') {
            h3 class: 'columnheader', 'Latest Guides'
            ul {
                List<Guide> latestGuides = guides.findAll {
                    it.publicationDate
                }.sort { Guide a, Guide b ->
                    b.publicationDate <=> a.publicationDate
                }.take(NUMBER_OF_LATEST_GUIDES)
                latestGuides.each { Guide guide ->
                    li {
                        b guide.title
                        span {
                            mkp.yield new SimpleDateFormat('MMM dd, yyyy').format(guide.publicationDate)

                            mkp.yield ' - '
                            mkp.yield guide.category
                        }
                        if (guide instanceof MultiLanguageGuide) {
                            MultiLanguageGuide multiLanguageGuide = ((MultiLanguageGuide) guide)
                            span guide.title
                            for (ProgrammingLanguage lang : multiLanguageGuide.githubSlugs.keySet()) {
                                a(style: 'display: inline;', class: 'lang', href: "https://guides.micronaut.io/${multiLanguageGuide.githubSlugs[lang].replaceAll('micronaut-guides/', '')}/guide/index.html") {
                                    mkp.yield(lang.name())
                                }
                            }
                        } else {
                            a href: "https://guides.micronaut.io/${guide.name}/guide/index.html", 'Read More'
                        }
                    }
                }
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String tagCloud() {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        html.div(class: 'tagsbytopic') {
            h3 class: 'columnheader', 'Guides by Tag'
            ul(class: 'tagcloud') {
                tags.sort { Tag a, Tag b -> a.slug <=> b.slug }.each { Tag t ->
                    li(class: "tag${t.ocurrence}") {
                        a href: "${guidesUrl()}/tags/${t.slug.toLowerCase()}.html", t.title
                    }
                }
            }
        }
        writer.toString()
    }

    @CompileDynamic
    String mainContent() {
        StringWriter writer = new StringWriter()
        MarkupBuilder html = new MarkupBuilder(writer)
        html.article {
            div(class: 'content container') {
                h1 {
                    span 'Micronaut'
                    b 'Guides'
                }
                setOmitEmptyAttributes(true)
                setOmitNullAttributes(true)
                String guideGroupCss = "margin-top: ${MARGIN_TOP};"
                div(class: 'twocolumns') {
                    div(class: 'column') {
                        div(class: 'mobile', style: 'margin-bottom: 50px;') {
                            mkp.yieldUnescaped searchBox('mobilequery')
                        }
                        div(id: 'searchresults') {
                            mkp.yieldUnescaped('')
                        }
                        if ( !(tag || category) ) {
                            mkp.yieldUnescaped latestGuides()
                        }
                        mkp.yieldUnescaped sponsoredBy()
                        if ( !(tag || category) ) {
                            mkp.yieldUnescaped tagCloud()
                            mkp.yieldUnescaped guideGroupByCategory(categories().cache, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().messaging, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().security, guides, true, guideGroupCss)
                        }

                    }
                    div(class: 'column') {
                        div(class: 'desktop') {
                            mkp.yieldUnescaped searchBox('query')
                        }
                        if ( tag ) {
                            mkp.yieldUnescaped guideGroupByTag(tag, guides)

                        } else if ( category ) {
                            mkp.yieldUnescaped guideGroupByCategory(category, guides.findAll { it.category == category.name }, false )

                        }
                        if ( !(tag || category) ) {
                            mkp.yieldUnescaped guideGroupByCategory(categories().apprentice, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().aws, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().azure, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().googlecloud, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().tracing, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().servicediscovery, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().cloudservices, guides, true, guideGroupCss)
                            mkp.yieldUnescaped guideGroupByCategory(categories().dataaccess, guides, true, guideGroupCss)

                        }
                    }
                }
            }
        }
        writer.toString()
    }

    static Map<String, Category> categories() {
        [
                servicediscovery: new Category(name: "Service Discovery", image: 'service-discovery.svg'),
                tracing: new Category(name: "Distributed Tracing", image: 'tracing.svg'),
                messaging: new Category(name: "Messaging", image: 'messaging.svg'),
                aws: new Category(name: "Micronaut + AWS", image: 'aws.svg'),
                azure: new Category(name: "Micronaut + Microsoft Azure", image: 'azure.svg'),
                googlecloud: new Category(name: "Micronaut + Google Cloud", image: 'googlecloud.svg'),
                android: new Category(name: "Micronaut Android", image: 'micronaut_android.svg'),
                devops: new Category(name: "Micronaut DevOps", image: 'micronaut_devops.svg'),
                apprentice: new Category(name: "Micronaut Apprentice", image: 'micronautaprrentice.svg'),
                cloudservices: new Category(name: 'Cloud Native', image: 'cloud.svg'),
                security: new Category(name: 'Micronaut Security', image: 'security.svg'),
                dataaccess: new Category(name: 'Data Access', image: 'dataaccess.svg'),
                cache: new Category(name: 'Cache', image: 'cache.svg'),
        ]
    }
}
