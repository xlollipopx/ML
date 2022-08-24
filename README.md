# ML arrival delay prediction
This project uses two regression machine learning algorithm to predict plane arrival delay.
Algorithms are linear regression and random forest regression
## Data Analysis
For the study was used dataset in here https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/HG7NV7.
In order to properly process the data, there are some variables from the
dataset that can’t be included since their values are only
known only when the plane has already taken off and all the algorithms
would than return inaccurate results. So, all such columns 
were dropped. 

To use regression algorithm we need to remove uncorrelated valiables to improve the model.

![image](https://user-images.githubusercontent.com/64196164/186499718-f2e6cab9-1947-4c7b-bf9c-057874f7599e.png)

Here we can see that ArrDelay has the stongest correlation with DepDelay and TexiOut.
Hence we will use them as independent variables in regression algorithms.

## Results
Before training, the data was split into a training and test set 70% and 30% respectively.
After comparing regression metrics(mean square error and mean absolute error) linear regression
gave better result than random forest regression.
