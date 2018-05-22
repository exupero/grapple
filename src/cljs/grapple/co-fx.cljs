(ns grapple.co-fx
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]))

(defn generate-ns-name []
  (let [adj ["affectionate" "amiable" "arrogant" "balmy" "barren" "benevolent" "billowing" "blessed" "breezy" "calm" "celestial" "charming" "combative" "composed" "condemned" "divine" "dry" "energized" "enigmatic" "exuberant" "flowing" "fluffy" "fluttering" "frightened" "fuscia" "gentle" "greasy" "grieving" "harmonious" "hollow" "homeless" "icy" "indigo" "inquisitive" "itchy" "joyful" "jubilant" "juicy" "khaki" "limitless" "lush" "mellow" "merciful" "merry" "mirthful" "moonlit" "mysterious" "natural" "outrageous" "pacific" "parched" "placid" "pleasant" "poised" "purring" "radioactive" "resilient" "scenic" "screeching" "sensitive" "serene" "snowy" "solitary" "spacial" "squealing" "stark" "stunning" "sunset" "talented" "tasteless" "teal" "thoughtless" "thriving" "tranquil" "tropical" "undisturbed" "unsightly" "unwavering" "uplifting" "voiceless" "wandering" "warm" "wealthy" "whispering" "withered" "wooden" "zealous"]
        things ["abyss" "atoll" "aurora" "autumn" "badlands" "beach" "briars" "brook" "canopy" "canyon" "cavern" "chasm" "cliff" "cove" "crater" "creek" "darkness" "dawn" "desert" "dew" "dove" "drylands" "dusk" "farm" "fern" "firefly" "flowers" "fog" "foliage" "forest" "galaxy" "garden" "geyser" "grove" "hurricane" "iceberg" "lagoon" "lake" "leaves" "marsh" "meadow" "mist" "moss" "mountain" "oasis" "ocean" "peak" "pebble" "pine" "plateau" "pond" "reef" "reserve" "resonance" "sanctuary" "sands" "shelter" "silence" "smokescreen" "snowflake" "spring" "storm" "stream" "summer" "summit" "sunrise" "sunset" "sunshine" "surf" "swamp" "temple" "thorns" "tsunami" "tundra" "valley" "volcano" "waterfall" "willow" "winds" "winter"]]
    (str (rand-nth adj) "." (rand-nth things))))

(rf/reg-cofx
  :generator/ns-name
  (fn [cofx _]
    (assoc cofx :generator/ns-name generate-ns-name)))

(rf/reg-cofx
  :generator/uuid
  (fn [cofx _]
    (assoc cofx :generator/uuid uuid/make-random-uuid)))
