(ns spid-docs.confluence-test
  (:require [midje.sweet :refer :all]
            [spid-docs.confluence :refer :all]
            [spid-docs.formatting :refer [to-html]]))

(fact (to-storage-format "<p>paragraph</p>") => "<p>paragraph</p>")

(fact (to-storage-format (to-html "External [link](http://example.com)"))
      => "<p>External <a href=\"http://example.com\">link</a></p>")

(fact (to-storage-format (to-html "Internal [link](#Working%20examples)"))
      => "<p>Internal <ac:link ac:anchor=\"Working examples\"><ac:plain-text-link-body><![CDATA[link]]></ac:plain-text-link-body></ac:link></p>")

(fact (to-storage-format (to-html "```php\n<?php echo 1; ?>\n```"))
      => "<ac:structured-macro ac:name=\"code\"><ac:parameter ac:name=\"language\">php</ac:parameter><ac:plain-text-body><![CDATA[<?php echo 1; ?>\n]]></ac:plain-text-body></ac:structured-macro>")

(fact
 "Unsupported languages are skipped to avoid confusing Confluence."
 (to-storage-format (to-html "```clj\n(+ 1 2 3)\n```"))
      => "<ac:structured-macro ac:name=\"code\"><ac:plain-text-body><![CDATA[(+ 1 2 3)\n]]></ac:plain-text-body></ac:structured-macro>")

(fact (to-storage-format
       (str "<div class=\"tabs\">"
              "<h3 class=\"tab\">One</h3>"
              "<div class=\"tab-content\"><p>Hello</p></div>"
              "<h3 class=\"tab\">Two</h3>"
              "<div class=\"tab-content\"><p>Hi there</p></div>"
            "</div>"))
      => (str "<ac:structured-macro ac:name=\"auitabs\">"
                "<ac:parameter ac:name=\"direction\">horizontal</ac:parameter>"
                "<ac:rich-text-body>"
                  "<ac:structured-macro ac:name=\"auitabspage\">"
                    "<ac:parameter ac:name=\"title\">One</ac:parameter>"
                    "<ac:rich-text-body><p>Hello</p></ac:rich-text-body>"
                  "</ac:structured-macro>"
                  "<ac:structured-macro ac:name=\"auitabspage\">"
                    "<ac:parameter ac:name=\"title\">Two</ac:parameter>"
                    "<ac:rich-text-body><p>Hi there</p></ac:rich-text-body>"
                  "</ac:structured-macro>"
                "</ac:rich-text-body>"
              "</ac:structured-macro>"))

(fact (to-storage-format
       (str "<div class=\"line\">"
              "<div class=\"unit s1of3\">1</div>"
              "<div class=\"unit s1of3\">2</div>"
              "<div class=\"lastUnit\">3</div>"
            "</div>"))
      => (str "<ac:layout>"
                "<ac:layout-section ac:type=\"three_equal\">"
                  "<ac:layout-cell>1</ac:layout-cell>"
                  "<ac:layout-cell>2</ac:layout-cell>"
                  "<ac:layout-cell>3</ac:layout-cell>"
                "</ac:layout-section>"
              "</ac:layout>"))
