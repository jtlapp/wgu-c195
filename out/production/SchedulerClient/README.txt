#### Software II Scheduling App ###

Submitted April 11, 2021

---- Purpose ----

This application is a demonstration desktop client for managing
customers and for scheduling appointments between customers and contacts
at a company. It is a Java client of a fixed-schema MySQL database written
in accordance with the requirements of the Software II C195 task.

---- Author ----

Joseph T. Lapp
Student #001361644
arachnojoe@gmail.com
512-296-6649

---- Versions ----

IntelliJ Community 2020.03
Java SE 11.0.10
JavaFX-SDK-11.0.2
mysql-connector-java-8.0.23

---- "How to run the program" ----

TBD: Make a package including all but JavaFX.

The easiest way to run the program is as follows:

(1) Open Intellij CE
(2) Select "IntelliJ IDEA" on the menu bar.
(3) Select "Preferences..."
(4) Select "Path Variables" on the left.
(5) Set variable PATH_TO_FX to the directory of your JavaFX library
    (e.g. /Library/javafx-sdk-11.0.2/lib).
(6) Select "Run" and then "Run 'Main'" on the menu bar.

To run the program from the command line using the supplied .class files:*

(1) Within a shell, cd to the project directory.
(2) Within the project directory, cd to out/production/SchedulerClient
(3) export PATH_TO_FX=/Library/javafx-sdk-11.0.2/lib
(4) export PATH_TO_MYSQL_DRIVER=/Library/mysql-connector-java-8.0.23
(5) java --module-path ${PATH_TO_FX}:${PATH_TO_MYSQL_DRIVER} --add-modules javafx.fxml,javafx.controls,javafx.graphics Main

* This should work on Mac and Linux/UNIX (bash shell). I don't know about Windows.

---- How to USE the program ----

The application opens with a login form asking for user credentials. Input
the credentials for any user in the database.

Upon successful login, the main window opens. It provides tabs for appointments,
customers, and reports. It also provides a user menu at the top right, with
options for logging out (getting a new login form) or exiting the program.

The appointments tab initially lists all appointments in the database at once.
The radio buttons above the table allow you to select from this view, a monthly
view, and a weekly view. The monthly and weekly views show one month or week at
a time. In these two views, the left and right arrow buttons at the top-right
of the table become active. Use them for paging by month or week. You may need
to page through empty screens of months or weeks not having appointments. Also,
if you're in one of these views and you delete the only appointment in that
view, rather than surprise you by jumping to another page, I require that you
navigate to existing appointments by these buttons, or else switch to viewing
all appointments (after which you can safely go back to monthly/weekly again).

The appointments tab also has buttons for adding, modifying, and deleting
appointments. The form for adding or modifying an appointment includes combo
boxes for the contact name, the customer name, and the start and end times.
The form supports the possibility that multiple contacts or multiple
customers have the same name by including the contact or customer's unique
database ID in parentheses after the name. The start and end time combo boxes
restrict you to only scheduling appointments from 8:00am to 10:00pm EST on
a single day. There is therefore only one date picker for an appointment.

The customers tab lists all customers in the database and provides buttons
for adding, modifying, and deleting customers. The form for adding or modifying
a customer includes combo boxes for country and "first level division" within
a country (labeled "division"). Selecting a country automatically restricts
the divisions available in the division combo box.

The reports tab provides a combo box listing three reports. Select any report
to have it displayed. Each report displays as a table. All of the reports
organize items into groups. To make these groupings more apparent at a glance,
the left-most column includes only single quotation marks to indicate that
the value of the cell is identical to the value of the cell above it.

Each of the tabs has a "Refresh" button. This button causes the application
to query the database for the data again to update the data on the tab for
any changes made to the database externally to the application.

All times are expressed in 24-hour "military" time for the user's time zone.

---- Lambdas ----

The JavaDoc comments include a number of paragraphs labelled "LAMBDA" to meet
the requirement that we justify in comments all lambdas appearing in the code.