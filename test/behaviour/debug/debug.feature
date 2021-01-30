#
# Copyright (C) 2021 Grakn Labs
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

Feature: Debugging Space

  Background: Set up databases for resolution testing
    Given connection has been opened
    Given connection does not have any database
    Given connection create database: reasoned
    Given connection create database: materialised
    Given connection open schema sessions for databases:
      | reasoned     |
      | materialised |
    Given for each session, open transactions of type: write
    Given for each session, graql define
      """
      define

      person sub entity,
        owns name,
        plays friendship:friend,
        plays employment:employee;

      company sub entity,
        owns name,
        plays employment:employer;

      place sub entity,
        owns name,
        plays location-hierarchy:subordinate,
        plays location-hierarchy:superior;

      friendship sub relation,
        relates friend;

      employment sub relation,
        relates employee,
        relates employer;

      location-hierarchy sub relation,
        relates subordinate,
        relates superior;

      name sub attribute, value string;
      """
    Given for each session, transaction commits
    Given for each session, open transactions of type: write


    # TODO this makes graph iterator fail!!!a When batch size is small, say 8


#  # TODO: re-enable all steps when materialisation is possible (may be an infinite graph?) (#75)
#  Scenario: when relations' and attributes' inferences are mutually recursive, the inferred concepts can be retrieved
#    Given for each session, graql define
#      """
#      define
#
#      word sub entity,
#          plays inheritance:subtype,
#          plays inheritance:supertype,
#          plays pair:prep,
#          plays pair:pobj,
#          owns name;
#
#      f sub word;
#      o sub word;
#
#      inheritance sub relation,
#          relates supertype,
#          relates subtype;
#
#      pair sub relation,
#          relates prep,
#          relates pobj,
#          owns typ,
#          owns name;
#
#      name sub attribute, value string;
#      typ sub attribute, value string;
#
#      rule inference-all-pairs: when {
#          $x isa word;
#          $y isa word;
#          $x has name != 'f';
#          $y has name != 'o';
#      } then {
#          (prep: $x, pobj: $y) isa pair;
#      };
#
#      rule inference-pairs-ff: when {
#          $f isa f;
#          (subtype: $prep, supertype: $f) isa inheritance;
#          (subtype: $pobj, supertype: $f) isa inheritance;
#          $p (prep: $prep, pobj: $pobj) isa pair;
#      } then {
#          $p has name 'ff';
#      };
#
#      rule inference-pairs-fo: when {
#          $f isa f;
#          $o isa o;
#          (subtype: $prep, supertype: $f) isa inheritance;
#          (subtype: $pobj, supertype: $o) isa inheritance;
#          $p (prep: $prep, pobj: $pobj) isa pair;
#      } then {
#          $p has name 'fo';
#      };
#      """
#    Given for each session, transaction commits
#    Given connection close all sessions
#    Given connection open data sessions for databases:
#      | reasoned     |
#      | materialised |
#    Given for each session, open transactions of type: write
#    Given for each session, graql insert
#    """
#      insert
#
#      $f isa f, has name "f";
#      $o isa o, has name "o";
#
#      $aa isa word, has name "aa";
#      $bb isa word, has name "bb";
#      $cc isa word, has name "cc";
#
#      (supertype: $o, subtype: $aa) isa inheritance;
#      (supertype: $o, subtype: $bb) isa inheritance;
#      (supertype: $o, subtype: $cc) isa inheritance;
#
#      $pp isa word, has name "pp";
#      $qq isa word, has name "qq";
#      $rr isa word, has name "rr";
#      $rr2 isa word, has name "rr";
#
#      (supertype: $f, subtype: $pp) isa inheritance;
#      (supertype: $f, subtype: $qq) isa inheritance;
#      (supertype: $f, subtype: $rr) isa inheritance;
#      (supertype: $f, subtype: $rr2) isa inheritance;
#      """
#    Given for each session, transaction commits
#    Given for each session, open transactions of type: write
#    Then materialised database is completed
#    Given for each session, transaction commits
#    Given for each session, open transactions with reasoning of type: read
#    Then for graql query
#      """
#      match $p isa pair, has name 'ff';
#      """
#    Then all answers are correct in reasoned database
#    Then answer size in reasoned database is: 16
#    Then for each session, transaction closes
#    Given for each session, open transactions with reasoning of type: read
#    Then for graql query
#      """
#      match $p isa pair;
#      """
#    Then all answers are correct in reasoned database
#    Then answer size in reasoned database is: 64
#    Then materialised and reasoned databases are the same size

#
#  Scenario: non-regular transitivity requiring iterative generation of tuples
#
#  from Vieille - Recursive Axioms in Deductive Databases p. 192
#
#    Given for each session, graql define
#      """
#      define
#
#      entity2 sub entity,
#        owns index,
#        plays R:role-A,
#        plays R:role-B,
#        plays E:role-A,
#        plays E:role-B,
#        plays F:role-A,
#        plays F:role-B,
#        plays G:role-A,
#        plays G:role-B,
#        plays H:role-A,
#        plays H:role-B;
#
#      R sub relation, relates role-A, relates role-B;
#
#      E sub relation, relates role-A, relates role-B;
#
#      F sub relation, relates role-A, relates role-B;
#
#      G sub relation, relates role-A, relates role-B;
#
#      H sub relation, relates role-A, relates role-B;
#
#      index sub attribute, value string;
#
#      rule rule-1: when {
#        (role-A: $x, role-B: $y) isa E;
#      } then {
#        (role-A: $x, role-B: $y) isa R;
#      };
#
#      rule rule-2: when {
#        (role-A: $x, role-B: $t) isa F;
#        (role-A: $t, role-B: $u) isa R;
#        (role-A: $u, role-B: $v) isa G;
#        (role-A: $v, role-B: $w) isa R;
#        (role-A: $w, role-B: $y) isa H;
#      } then {
#        (role-A: $x, role-B: $y) isa R;
#      };
#      """
#    Given for each session, transaction commits
#    Given connection close all sessions
#    Given connection open data sessions for databases:
#      | reasoned     |
#      | materialised |
#    Given for each session, open transactions of type: write
#    Given for each session, graql insert
#    """
#      insert
#
#      $i isa entity2, has index "i";
#      $j isa entity2, has index "j";
#      $k isa entity2, has index "k";
#      $l isa entity2, has index "l";
#      $m isa entity2, has index "m";
#      $n isa entity2, has index "n";
#      $o isa entity2, has index "o";
#      $p isa entity2, has index "p";
#      $q isa entity2, has index "q";
#      $r isa entity2, has index "r";
#      $s isa entity2, has index "s";
#      $t isa entity2, has index "t";
#      $u isa entity2, has index "u";
#      $v isa entity2, has index "v";
#
#      (role-A: $i, role-B: $j) isa E;
#      (role-A: $l, role-B: $m) isa E;
#      (role-A: $n, role-B: $o) isa E;
#      (role-A: $q, role-B: $r) isa E;
#      (role-A: $t, role-B: $u) isa E;
#
#      (role-A: $i, role-B: $i) isa F;
#      (role-A: $i, role-B: $k) isa F;
#      (role-A: $k, role-B: $l) isa F;
#
#      (role-A: $m, role-B: $n) isa G;
#      (role-A: $p, role-B: $q) isa G;
#      (role-A: $s, role-B: $t) isa G;
#
#      (role-A: $o, role-B: $p) isa H;
#      (role-A: $r, role-B: $s) isa H;
#      (role-A: $u, role-B: $v) isa H;
#      """
#    Then materialised database is completed
#    Given for each session, transaction commits
#    Given for each session, open transactions with reasoning of type: read
#    Then for graql query
#      """
#      match
#        ($x, $y) isa R;
#        $x has index 'i';
#      get $y;
#      """
#    Then answer size in reasoned database is: 3
#    Given for each session, transaction closes
#    Given for each session, open transactions of type: write
#    Then materialised database is completed
#    Given for each session, transaction closes
#    Given for each session, open transactions with reasoning of type: read
#    Then answer set is equivalent for graql query
#      """
#      match
#        $y has index $ind;
#        {$ind = 'j';} or {$ind = 's';} or {$ind = 'v';};
#      get $y;
#      """
#    Then materialised and reasoned databases are the same size
#


#  # TODO fails to find some answers
#  Scenario: reverse same-generation test
#
#  from Abiteboul - Foundations of databases p. 312/Cao test 6.14 p. 89
#
#    Given for each session, graql define
#      """
#      define
#
#      person sub entity,
#        owns name,
#        plays parentship:parent,
#        plays parentship:child,
#        plays RevSG:from,
#        plays RevSG:to,
#        plays up:from,
#        plays up:to,
#        plays down:from,
#        plays down:to,
#        plays flat:from,
#        plays flat:to;
#
#      parentship sub relation, relates parent, relates child;
#
#      RevSG sub relation, relates from, relates to;
#
#      up sub relation, relates from, relates to;
#
#      down sub relation, relates from, relates to;
#
#      flat sub relation, relates to, relates from;
#
#      name sub attribute, value string;
#
#      rule rule-1: when {
#        (from: $x, to: $y) isa flat;
#      } then {
#        (from: $x, to: $y) isa RevSG;
#      };
#
#      rule rule-2: when {
#        (from: $x, to: $x1) isa up;
#        (from: $y1, to: $x1) isa RevSG;
#        (from: $y1, to: $y) isa down;
#      } then {
#        (from: $x, to: $y) isa RevSG;
#      };
#      """
#    Given for each session, transaction commits
#    Given connection close all sessions
#    Given connection open data sessions for databases:
#      | reasoned     |
#      | materialised |
#    Given for each session, open transactions of type: write
#    Given for each session, graql insert
#    """
#      insert
#
#      $a isa person, has name "a";
#      $b isa person, has name "b";
#      $c isa person, has name "c";
#      $d isa person, has name "d";
#      $e isa person, has name "e";
#      $f isa person, has name "f";
#      $g isa person, has name "g";
#      $h isa person, has name "h";
#      $i isa person, has name "i";
#      $j isa person, has name "j";
#      $k isa person, has name "k";
#      $l isa person, has name "l";
#      $m isa person, has name "m";
#      $n isa person, has name "n";
#      $o isa person, has name "o";
#      $p isa person, has name "p";
#
#      (from: $a, to: $e) isa up;
#      (from: $a, to: $f) isa up;
#      (from: $f, to: $m) isa up;
#      (from: $g, to: $n) isa up;
#      (from: $h, to: $n) isa up;
#      (from: $i, to: $o) isa up;
#      (from: $j, to: $o) isa up;
#
#      (from: $g, to: $f) isa flat;
#      (from: $m, to: $n) isa flat;
#      (from: $m, to: $o) isa flat;
#      (from: $p, to: $m) isa flat;
#
#      (from: $l, to: $f) isa down;
#      (from: $m, to: $f) isa down;
#      (from: $g, to: $b) isa down;
#      (from: $h, to: $c) isa down;
#      (from: $i, to: $d) isa down;
#      (from: $p, to: $k) isa down;
#      """
#    Given for each session, transaction commits
#    Given for each session, open transactions of type: write
#    Then materialised database is completed
#    Given for each session, transaction commits
#    Given for each session, open transactions with reasoning of type: read
#    Then for graql query
#      """
#      match
#        (from: $x, to: $y) isa RevSG;
#        $x has name 'a';
#      get $y;
#      """
#    Then answer size in reasoned database is: 3
#    Then all answers are correct in reasoned database
#    Given for each session, transaction closes
#    Given for each session, open transactions with reasoning of type: read
#    Then answer set is equivalent for graql query
#      """
#      match
#        $y isa person, has name $name;
#        {$name = 'b';} or {$name = 'c';} or {$name = 'd';};
#      get $y;
#      """
#    Given for each session, transaction closes
#    Given for each session, open transactions with reasoning of type: read
#    Then for graql query
#      """
#      match (from: $x, to: $y) isa RevSG;
#      """
#    Then answer size in reasoned database is: 11
#    Then all answers are correct in reasoned database
#    Given for each session, transaction closes
#    Given for each session, open transactions with reasoning of type: read
#    Then answer set is equivalent for graql query
#      """
#      match
#        $x has name $nameX;
#        $y has name $nameY;
#        {$nameX = 'a';$nameY = 'b';} or {$nameX = 'a';$nameY = 'c';} or
#        {$nameX = 'a';$nameY = 'd';} or {$nameX = 'm';$nameY = 'n';} or
#        {$nameX = 'm';$nameY = 'o';} or {$nameX = 'p';$nameY = 'm';} or
#        {$nameX = 'g';$nameY = 'f';} or {$nameX = 'h';$nameY = 'f';} or
#        {$nameX = 'i';$nameY = 'f';} or {$nameX = 'j';$nameY = 'f';} or
#        {$nameX = 'f';$nameY = 'k';};
#      get $x, $y;
#      """
#    Then materialised and reasoned databases are the same size



