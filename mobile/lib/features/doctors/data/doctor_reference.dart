import '../../../core/l10n/l10n.dart';

/// The 24 governorate codes (backend enum names), in the same order as the
/// backend `Governorate` enum.
const List<String> kGovernorates = [
  'TUNIS',
  'ARIANA',
  'BEN_AROUS',
  'MANOUBA',
  'NABEUL',
  'ZAGHOUAN',
  'BIZERTE',
  'BEJA',
  'JENDOUBA',
  'KEF',
  'SILIANA',
  'SOUSSE',
  'MONASTIR',
  'MAHDIA',
  'SFAX',
  'KAIROUAN',
  'KASSERINE',
  'SIDI_BOUZID',
  'GABES',
  'MEDENINE',
  'TATAOUINE',
  'GAFSA',
  'TOZEUR',
  'KEBILI',
];

/// Spoken-language codes a doctor can advertise (backend `SpokenLanguage`).
const List<String> kSpokenLanguages = ['AR', 'FR', 'EN'];

/// Localized display name for a governorate [code], or empty string if unknown.
String governorateLabel(AppLocalizations l10n, String? code) {
  switch (code) {
    case 'TUNIS':
      return l10n.govTunis;
    case 'ARIANA':
      return l10n.govAriana;
    case 'BEN_AROUS':
      return l10n.govBenArous;
    case 'MANOUBA':
      return l10n.govManouba;
    case 'NABEUL':
      return l10n.govNabeul;
    case 'ZAGHOUAN':
      return l10n.govZaghouan;
    case 'BIZERTE':
      return l10n.govBizerte;
    case 'BEJA':
      return l10n.govBeja;
    case 'JENDOUBA':
      return l10n.govJendouba;
    case 'KEF':
      return l10n.govKef;
    case 'SILIANA':
      return l10n.govSiliana;
    case 'SOUSSE':
      return l10n.govSousse;
    case 'MONASTIR':
      return l10n.govMonastir;
    case 'MAHDIA':
      return l10n.govMahdia;
    case 'SFAX':
      return l10n.govSfax;
    case 'KAIROUAN':
      return l10n.govKairouan;
    case 'KASSERINE':
      return l10n.govKasserine;
    case 'SIDI_BOUZID':
      return l10n.govSidiBouzid;
    case 'GABES':
      return l10n.govGabes;
    case 'MEDENINE':
      return l10n.govMedenine;
    case 'TATAOUINE':
      return l10n.govTataouine;
    case 'GAFSA':
      return l10n.govGafsa;
    case 'TOZEUR':
      return l10n.govTozeur;
    case 'KEBILI':
      return l10n.govKebili;
    default:
      return '';
  }
}

/// Localized display name for a spoken-language [code], or the raw code if unknown.
String languageLabel(AppLocalizations l10n, String code) {
  switch (code) {
    case 'AR':
      return l10n.languageAr;
    case 'FR':
      return l10n.languageFr;
    case 'EN':
      return l10n.languageEn;
    default:
      return code;
  }
}
